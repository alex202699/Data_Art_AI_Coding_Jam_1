import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApiService } from '../../core/auth-api.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    @if (done()) {
      <div class="auth-card center">
        <div class="badge-check">✓</div>
        <h1>Check your email</h1>
        <p class="subtitle">
          If that account exists, we sent a password reset link. The link expires in 1 hour.
        </p>
        <p class="link-row"><a routerLink="/login">Back to log in →</a></p>
      </div>
    } @else {
      <div class="auth-card">
        <h1>Reset password</h1>
        <p class="subtitle">Enter your email and we'll send you a reset link.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" autocomplete="email"
                   placeholder="name@example.com" formControlName="email" />
          </div>
          <button class="btn" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Sending…' : 'Send reset link' }}
          </button>
        </form>

        <p class="link-row"><a routerLink="/login">Back to log in →</a></p>
      </div>
    }
  `,
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly submitting = signal(false);
  readonly done = signal(false);

  async submit(): Promise<void> {
    if (this.form.invalid) {
      return;
    }
    this.submitting.set(true);
    try {
      await firstValueFrom(this.api.requestPasswordReset(this.form.getRawValue().email));
    } finally {
      // Response is intentionally generic (anti-enumeration).
      this.submitting.set(false);
      this.done.set(true);
    }
  }
}
