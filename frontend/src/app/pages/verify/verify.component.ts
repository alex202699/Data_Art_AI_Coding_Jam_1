import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApiService } from '../../core/auth-api.service';

type Status = 'loading' | 'verified' | 'invalid';

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="auth-card center">
          <h1>Verifying…</h1>
          <p class="subtitle">Please wait a moment.</p>
        </div>
      }
      @case ('verified') {
        <div class="auth-card center">
          <div class="badge-check">✓</div>
          <h1>Email verified</h1>
          <p class="subtitle">Your account is ready to use.</p>
          <button class="btn" type="button" (click)="goToLogin()">Continue to login</button>
        </div>
      }
      @default {
        <div class="auth-card center">
          <h1>Link expired or invalid</h1>
          <p class="subtitle">
            This verification link is no longer valid. Enter your email to get a new one.
          </p>
          @if (resent()) {
            <p class="notice">If that account is unverified, a new email is on its way.</p>
          }
          <div class="field">
            <input type="email" placeholder="name@example.com" [(ngModel)]="email" />
          </div>
          <button class="btn" type="button" (click)="resend()">Resend verification email</button>
          <p class="link-row"><a routerLink="/login">Back to log in →</a></p>
        </div>
      }
    }
  `,
})
export class VerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AuthApiService);

  readonly status = signal<Status>('loading');
  readonly resent = signal(false);
  email = '';

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!token) {
      this.status.set('invalid');
      return;
    }
    try {
      const res = await firstValueFrom(this.api.verify(token));
      this.status.set(res.status === 'verified' ? 'verified' : 'invalid');
    } catch {
      this.status.set('invalid');
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  async resend(): Promise<void> {
    this.resent.set(false);
    try {
      await firstValueFrom(this.api.resendVerification(this.email));
    } finally {
      this.resent.set(true);
    }
  }
}
