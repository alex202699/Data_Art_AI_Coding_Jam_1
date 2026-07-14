import { Component } from '@angular/core';

@Component({
  selector: 'app-epics',
  standalone: true,
  template: `
    <h1>Epics</h1>
    <p class="muted">Epics belong to a team chosen at creation.</p>
    <!-- TODO(frontend-engineer): team selector, list epics, create/edit,
         delete (disabled + clear message while tickets reference the epic -> 409). -->
  `,
  styles: [`.muted { color: var(--muted); }`],
})
export class EpicsComponent {}
