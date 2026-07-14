import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { User } from './models';

const TOKEN_KEY = 'ticketing.token';

/**
 * Holds the bearer token + current user. The token is a transient session credential
 * (kept in sessionStorage so a refresh survives), not the system of record — all
 * persistent data lives in the backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApiService);

  readonly token = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));
  readonly user = signal<User | null>(null);
  readonly isAuthenticated = computed(() => this.token() !== null);

  /** Log in, store the token, and load the current user. */
  async login(email: string, password: string): Promise<void> {
    const res = await firstValueFrom(this.api.login(email, password));
    this.setToken(res.token);
    this.user.set(res.user);
  }

  /** On app start, if a token is present, resolve the current user (clears on failure). */
  async bootstrap(): Promise<void> {
    if (!this.token()) {
      return;
    }
    try {
      this.user.set(await firstValueFrom(this.api.me()));
    } catch {
      this.clear();
    }
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.api.logout());
    } finally {
      this.clear();
    }
  }

  setToken(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    this.token.set(token);
  }

  clear(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
    this.user.set(null);
  }
}
