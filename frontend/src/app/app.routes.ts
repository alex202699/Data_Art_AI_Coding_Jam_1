import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';
import { AppShellComponent } from './layouts/app-shell.component';
import { AuthShellComponent } from './layouts/auth-shell.component';

export const routes: Routes = [
  // Land on the board; the guard bounces unauthenticated users to /login.
  { path: '', pathMatch: 'full', redirectTo: 'board' },

  // Public auth screens (centered card, no header).
  {
    path: '',
    component: AuthShellComponent,
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./pages/login/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'signup',
        loadComponent: () =>
          import('./pages/signup/signup.component').then((m) => m.SignupComponent),
      },
      {
        path: 'verify',
        loadComponent: () =>
          import('./pages/verify/verify.component').then((m) => m.VerifyComponent),
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./pages/forgot-password/forgot-password.component').then(
            (m) => m.ForgotPasswordComponent,
          ),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./pages/reset-password/reset-password.component').then(
            (m) => m.ResetPasswordComponent,
          ),
      },
    ],
  },

  // Authenticated app screens (header + gray canvas).
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'board',
        loadComponent: () =>
          import('./pages/board/board.component').then((m) => m.BoardComponent),
      },
      {
        path: 'teams',
        loadComponent: () =>
          import('./pages/teams/teams.component').then((m) => m.TeamsComponent),
      },
      {
        path: 'epics',
        loadComponent: () =>
          import('./pages/epics/epics.component').then((m) => m.EpicsComponent),
      },
      {
        path: 'tickets/new',
        data: { mode: 'create' },
        loadComponent: () =>
          import('./pages/ticket-editor/ticket-editor.component').then((m) => m.TicketEditorComponent),
      },
      {
        path: 'tickets/:id',
        data: { mode: 'edit' },
        loadComponent: () =>
          import('./pages/ticket-editor/ticket-editor.component').then((m) => m.TicketEditorComponent),
      },
      { path: '', pathMatch: 'full', redirectTo: 'board' },
    ],
  },

  { path: '**', redirectTo: 'board' },
];
