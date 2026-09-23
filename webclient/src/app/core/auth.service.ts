import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

interface UserRepr {
  userName: string;
  roles: string[];
}

/** Server-confirmed identity; authentication stays in the HttpOnly session cookie. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly userName = signal<string | null>(null);
  readonly roles = signal<string[]>([]);

  constructor() { sessionStorage.removeItem('acb.auth'); }

  get isLoggedIn(): boolean { return this.userName() !== null; }

  async restoreSession(): Promise<void> {
    try {
      await this.refreshCsrf();
      await this.loadIdentity();
    } catch {
      this.clearIdentity();
    }
  }

  async login(username: string, password: string): Promise<void> {
    this.clearIdentity();
    await this.refreshCsrf();
    const body = new HttpParams().set('username', username).set('password', password);
    await firstValueFrom(this.http.post('/api/auth/login', body));
    await this.refreshCsrf();
    await this.loadIdentity();
  }

  async logout(): Promise<void> {
    await this.refreshCsrf();
    await firstValueFrom(this.http.post('/api/auth/logout', null));
    this.clearIdentity();
    // Logout succeeded even if the next CSRF bootstrap is temporarily unavailable.
    await this.refreshCsrf().catch(() => undefined);
  }

  clearIdentity(): void {
    this.userName.set(null);
    this.roles.set([]);
  }

  private async refreshCsrf(): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
  }

  private async loadIdentity(): Promise<void> {
    const user = await firstValueFrom(this.http.get<UserRepr>('/api/auth/me'));
    this.userName.set(user.userName);
    this.roles.set(user.roles ?? []);
  }
}
