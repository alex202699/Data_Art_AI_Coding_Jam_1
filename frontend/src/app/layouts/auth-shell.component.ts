import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Centered-card wrapper for the public auth screens (no app header). */
@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="auth-shell">
      <router-outlet />
    </div>
  `,
})
export class AuthShellComponent {}
