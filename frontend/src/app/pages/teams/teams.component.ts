import { Component } from '@angular/core';

@Component({
  selector: 'app-teams',
  standalone: true,
  template: `
    <h1>Teams</h1>
    <p class="muted">All verified users can view and manage all teams.</p>
    <!-- TODO(frontend-engineer): list teams with ticket/epic counts + modified time,
         create/rename, and delete (disabled + clear message when the team has tickets
         or epics -> backend returns 409). -->
  `,
  styles: [`.muted { color: var(--muted); }`],
})
export class TeamsComponent {}
