import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApiService } from '../../core/auth-api.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-card">
      <h1>Log in</h1>
      <p class="subtitle">Use your verified account.</p>

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
      @if (resent()) {
        <p class="notice">Verification email sent — check your inbox.</p>
      }

      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field">
          <label for="email">Email</label>
          <input id="email" type="email" autocomplete="email"
                 placeholder="name@example.com" formControlName="email" />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" autocomplete="current-password"
                 formControlName="password" />
        </div>
        <button class="btn" type="submit" [disabled]="submitting()">
          {{ submitting() ? 'Logging in…' : 'Log in' }}
        </button>
      </form>

      @if (unverified()) {
        <p class="notice">
          Account not verified?
          <button type="button" class="linklike" (click)="resend()">Resend email</button>
        </p>
      }

      <p class="link-row"><a routerLink="/forgot-password">Forgot password?</a></p>
      <p class="link-row"><a routerLink="/signup">Create an account →</a></p>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly api = inject(AuthApiService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly unverified = signal(false);
  readonly resent = signal(false);

  async submit(): Promise<void> {
    if (this.form.invalid) {
      return;
    }
    this.error.set(null);
    this.unverified.set(false);
    this.resent.set(false);
    this.submitting.set(true);
    const { email, password } = this.form.getRawValue();
    try {
      await this.auth.login(email, password);
      this.router.navigate(['/board']);
    } catch (err) {
      if (err instanceof HttpErrorResponse && err.status === 403) {
        this.unverified.set(true);
        this.error.set('Your account is not verified yet.');
      } else if (err instanceof HttpErrorResponse && err.status === 401) {
        this.error.set('Invalid email or password.');
      } else {
        this.error.set('Something went wrong. Please try again.');
      }
    } finally {
      this.submitting.set(false);
    }
  }

  async resend(): Promise<void> {
    this.resent.set(false);
    try {
      await firstValueFrom(this.api.resendVerification(this.form.getRawValue().email));
    } finally {
      // Response is intentionally generic.
      this.resent.set(true);
    }
  }
}
