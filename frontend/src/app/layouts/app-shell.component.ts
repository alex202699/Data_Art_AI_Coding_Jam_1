import { Component, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/auth.service';

/** Authenticated app shell: top header (brand, nav, user menu) over a gray content canvas. */
@Component({
  selector: 'app-app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="app-header">
      <span class="brand">TICKET TRACKER</span>
      <nav>
        <a routerLink="/board" routerLinkActive="active">Board</a>
        <a routerLink="/teams" routerLinkActive="active">Teams</a>
        <a routerLink="/epics" routerLinkActive="active">Epics</a>
      </nav>
      <span class="spacer"></span>
      <div class="user-menu">
        <button type="button" (click)="toggleMenu($event)">
          {{ email() }} ▾
        </button>
        @if (menuOpen()) {
          <div class="dropdown">
            <button type="button" (click)="logout()">Log out</button>
          </div>
        }
      </div>
    </header>
    <main class="app-main">
      <router-outlet />
    </main>
  `,
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly menuOpen = signal(false);

  email(): string {
    return this.auth.user()?.email ?? 'Account';
  }

  toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.menuOpen.update((open) => !open);
  }

  @HostListener('document:click')
  closeMenu(): void {
    this.menuOpen.set(false);
  }

  async logout(): Promise<void> {
    this.menuOpen.set(false);
    await this.auth.logout();
    this.router.navigate(['/login']);
  }
}
