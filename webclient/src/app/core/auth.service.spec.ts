import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { MessagesService } from './messages.service';

describe('Session authentication', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem('acb.auth', 'obsolete-basic-credentials');
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([]),
    ] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    spyOn(TestBed.inject(MessagesService), 'error');
  });
  afterEach(() => http.verify());

  it('removes old credentials and establishes identity only after server login', fakeAsync(() => {
    expect(sessionStorage.getItem('acb.auth')).toBeNull();
    void auth.login('alice', 'secret');
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body.get('username')).toBe('alice');
    expect(login.request.headers.has('Authorization')).toBeFalse();
    expect(auth.isLoggedIn).toBeFalse();
    login.flush(null, { status: 204, statusText: 'No Content' });
    flushMicrotasks();
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
    http.expectOne('/api/auth/me').flush({ userName: 'alice', roles: ['ROLE_USER'] });
    flushMicrotasks();
    expect(auth.userName()).toBe('alice');
    expect(auth.roles()).toEqual(['ROLE_USER']);
    expect(sessionStorage.getItem('acb.auth')).toBeNull();
  }));

  it('restores a cookie session without stored credentials', fakeAsync(() => {
    void auth.restoreSession();
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
    http.expectOne('/api/auth/me').flush({ userName: 'bob', roles: ['ROLE_USER'] });
    flushMicrotasks();
    expect(auth.isLoggedIn).toBeTrue();
  }));

  it('clears expired sessions on 401', fakeAsync(() => {
    auth.userName.set('alice');
    void auth.restoreSession();
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    flushMicrotasks();
    expect(auth.isLoggedIn).toBeFalse();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login']);
  }));

  it('awaits logout and preserves identity if the protected request fails', fakeAsync(() => {
    auth.userName.set('alice');
    let failed = false;
    void auth.logout().catch(() => { failed = true; });
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
    expect(auth.isLoggedIn).toBeTrue();
    http.expectOne('/api/auth/logout').flush({ error: 'csrf' }, { status: 403, statusText: 'Forbidden' });
    flushMicrotasks();
    expect(failed).toBeTrue();
    expect(auth.isLoggedIn).toBeTrue();
    expect(TestBed.inject(MessagesService).error).toHaveBeenCalled();
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  }));

  it('clears identity after successful server logout', fakeAsync(() => {
    auth.userName.set('alice');
    void auth.logout();
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
    http.expectOne('/api/auth/logout').flush(null);
    flushMicrotasks();
    expect(auth.isLoggedIn).toBeFalse();
    http.expectOne('/api/auth/csrf').flush(null);
    flushMicrotasks();
  }));
});
