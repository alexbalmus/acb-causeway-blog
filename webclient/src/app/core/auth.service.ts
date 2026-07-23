import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

const STORAGE_KEY = 'acb.auth';

interface UserRepr {
  userName: string;
  roles: string[];
}

/**
 * Holds the HTTP Basic credentials for the Causeway REST API.
 * Credentials are verified against GET /restful/user and kept in
 * sessionStorage so a page reload does not log the user out.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly userName = signal<string | null>(null);
  readonly roles = signal<string[]>([]);

  /** base64("user:pass") or null when not logged in */
  get credentials(): string | null {
    return sessionStorage.getItem(STORAGE_KEY);
  }

  get isLoggedIn(): boolean {
    return this.credentials !== null;
  }

  /** Re-validates stored credentials on app start; clears them if stale. */
  async restoreSession(): Promise<void> {
    if (!this.credentials) {
      return;
    }
    try {
      const user = await firstValueFrom(this.http.get<UserRepr>('/restful/user'));
      this.userName.set(user.userName);
      this.roles.set(user.roles ?? []);
    } catch {
      this.logout();
    }
  }

  async login(username: string, password: string): Promise<void> {
    const encoded = btoa(`${username}:${password}`);
    const headers = new HttpHeaders({ Authorization: `Basic ${encoded}` });
    const user = await firstValueFrom(
      this.http.get<UserRepr>('/restful/user', { headers }),
    );
    sessionStorage.setItem(STORAGE_KEY, encoded);
    this.userName.set(user.userName);
    this.roles.set(user.roles ?? []);
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.userName.set(null);
    this.roles.set([]);
  }
}
