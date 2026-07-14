import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApiService } from '../../core/auth-api.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    @if (!token()) {
      <div class="auth-card center">
        <h1>Invalid reset link</h1>
        <p class="subtitle">This password reset link is missing or malformed.</p>
        <p class="link-row"><a routerLink="/forgot-password">Request a new link →</a></p>
      </div>
    } @else if (done()) {
      <div class="auth-card center">
        <div class="badge-check">✓</div>
        <h1>Password updated</h1>
        <p class="subtitle">You can now log in with your new password.</p>
        <button class="btn" type="button" (click)="goToLogin()">Continue to login</button>
      </div>
    } @else {
      <div class="auth-card">
        <h1>Set a new password</h1>
        <p class="subtitle">Choose a password with at least 8 characters.</p>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="password">New password</label>
            <input id="password" type="password" autocomplete="new-password"
                   placeholder="Minimum 8 characters" formControlName="password" />
          </div>
          <div class="field">
            <label for="confirm">Confirm password</label>
            <input id="confirm" type="password" autocomplete="new-password"
                   formControlName="confirm" />
          </div>
          <button class="btn" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Updating…' : 'Update password' }}
          </button>
        </form>

        <p class="link-row"><a routerLink="/login">Back to log in →</a></p>
      </div>
    }
  `,
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirm: ['', [Validators.required]],
  });

  readonly token = signal('');
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly done = signal(false);

  ngOnInit(): void {
    this.token.set(this.route.snapshot.queryParamMap.get('token') ?? '');
  }

  async submit(): Promise<void> {
    this.error.set(null);
    const { password, confirm } = this.form.getRawValue();
    if (password.length < 8) {
      this.error.set('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      this.error.set('Passwords do not match.');
      return;
    }
    this.submitting.set(true);
    try {
      await firstValueFrom(this.api.resetPassword(this.token(), password));
      this.done.set(true);
    } catch (err) {
      if (err instanceof HttpErrorResponse && err.status === 400) {
        this.error.set('This reset link is invalid or has expired.');
      } else {
        this.error.set('Something went wrong. Please try again.');
      }
    } finally {
      this.submitting.set(false);
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
