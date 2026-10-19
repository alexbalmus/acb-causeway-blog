import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { authGuard } from './auth.guard';

describe('Authentication HTTP integration', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]),
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); sessionStorage.clear(); });

  it('stores credentials only after the server accepts them, then restores identity', async () => {
    const login = auth.login('writer', 'secret');
    expect(auth.isLoggedIn).toBeFalse();
    const probe = http.expectOne('/restful/user/');
    expect(probe.request.headers.get('Authorization')).toBe('Basic ' + btoa('writer:secret'));
    probe.flush({ userName: 'writer', roles: ['author'] });
    await login;
    expect(auth.userName()).toBe('writer');
    auth.userName.set(null);
    const restore = auth.restoreSession();
    const request = http.expectOne('/restful/user/');
    expect(request.request.headers.get('Authorization')).toBe('Basic ' + btoa('writer:secret'));
    request.flush({ userName: 'writer', roles: ['author'] });
    await restore;
    expect(auth.roles()).toEqual(['author']);
    expect(auth.userName()).toBe('writer');
  });

  it('rejects an invalid login without storing credentials or redirecting the login probe', async () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate');
    const login = auth.login('writer', 'wrong');
    const rejected = expectAsync(login).toBeRejected();
    http.expectOne('/restful/user/').flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });
    await rejected;
    expect(auth.isLoggedIn).toBeFalse();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('clears a stale session and redirects when an authenticated request returns 401', async () => {
    sessionStorage.setItem('acb.auth', btoa('writer:old'));
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const restore = auth.restoreSession();
    http.expectOne('/restful/user/').flush('', { status: 401, statusText: 'Unauthorized' });
    await restore;
    expect(auth.credentials).toBeNull();
    expect(auth.userName()).toBeNull();
    expect(auth.roles()).toEqual([]);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('keeps credentials off static asset requests and preserves an explicit Authorization header', async () => {
    sessionStorage.setItem('acb.auth', 'stored');
    const client = TestBed.inject(HttpClient);
    const asset = firstValueFrom(client.get('/blog/assets/config.json'));
    const request = http.expectOne('/blog/assets/config.json');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({}); await asset;
    const explicit = firstValueFrom(client.get('/restful/user/', { headers: { Authorization: 'Basic explicit' } }));
    const probe = http.expectOne('/restful/user/');
    expect(probe.request.headers.get('Authorization')).toBe('Basic explicit');
    probe.flush({}); await explicit;
  });

  it('guards private routes and allows them after login', () => {
    const run = () => TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(String(run())).toBe('/login');
    sessionStorage.setItem('acb.auth', 'stored');
    expect(run()).toBeTrue();
    auth.logout();
    expect(String(run())).toBe('/login');
  });
});
