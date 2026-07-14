import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { LoginResponse, MessageResponse, User, VerifyResponse } from './models';

/** Calls the backend auth endpoints under /api/auth. */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/auth`;

  signup(email: string, password: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/signup`, { email, password });
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, { email, password });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, {});
  }

  verify(token: string): Observable<VerifyResponse> {
    return this.http.get<VerifyResponse>(`${this.base}/verify`, { params: { token } });
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/resend-verification`, { email });
  }

  requestPasswordReset(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/request-password-reset`, { email });
  }

  resetPassword(token: string, password: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/reset-password`, { token, password });
  }

  me(): Observable<User> {
    return this.http.get<User>(`${this.base}/me`);
  }
}
