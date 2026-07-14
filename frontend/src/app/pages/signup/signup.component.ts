import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApiService } from '../../core/auth-api.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    @if (done()) {
      <div class="auth-card center">
        <h1>Check your email</h1>
        <p class="subtitle">
          We sent a verification link to <strong>{{ submittedEmail() }}</strong>.
          Verify within 24 hours to activate your account.
        </p>
        <button type="button" class="btn btn-secondary" (click)="resend()">Resend email</button>
        @if (resent()) {
          <p class="notice">Verification email sent — check your inbox.</p>
        }
        <p class="link-row"><a routerLink="/login">Back to log in →</a></p>
      </div>
    } @else {
      <div class="auth-card">
        <h1>Create account</h1>
        <p class="subtitle">Email verification is required.</p>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" autocomplete="email"
                   placeholder="name@example.com" formControlName="email" />
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input id="password" type="password" autocomplete="new-password"
                   placeholder="Minimum 8 characters" formControlName="password" />
          </div>
          <div class="field">
            <label for="confirm">Confirm password</label>
            <input id="confirm" type="password" autocomplete="new-password"
                   formControlName="confirm" />
          </div>
          <button class="btn" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Creating…' : 'Sign up' }}
          </button>
        </form>

        <p class="link-row"><a routerLink="/login">Already registered? Log in →</a></p>
      </div>
    }
  `,
})
export class SignupComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirm: ['', [Validators.required]],
  });

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly done = signal(false);
  readonly resent = signal(false);
  readonly submittedEmail = signal('');

  async submit(): Promise<void> {
    this.error.set(null);
    const { email, password, confirm } = this.form.getRawValue();

    if (password.length < 8) {
      this.error.set('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      this.error.set('Passwords do not match.');
      return;
    }
    if (this.form.controls.email.invalid) {
      this.error.set('Enter a valid email address.');
      return;
    }

    this.submitting.set(true);
    try {
      await firstValueFrom(this.api.signup(email, password));
      this.submittedEmail.set(email);
      this.done.set(true);
    } catch (err) {
      if (err instanceof HttpErrorResponse && err.status === 409) {
        this.error.set('That email is already registered.');
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
      await firstValueFrom(this.api.resendVerification(this.submittedEmail()));
    } finally {
      this.resent.set(true);
    }
  }
}
