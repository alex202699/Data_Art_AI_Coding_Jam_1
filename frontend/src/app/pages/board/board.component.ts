import { Component } from '@angular/core';
import { TICKET_STATES, TICKET_STATE_LABELS, TicketState } from '../../core/models';

@Component({
  selector: 'app-board',
  standalone: true,
  template: `
    <h1>Board</h1>
    <p class="muted">Team selector, filters, search and draggable cards go here.</p>
    <!-- TODO(frontend-engineer): team selector, type/epic filters + title search (AND),
         New ticket action, and CDK drag-drop that persists state changes via the API
         (revert the card + show an error on failure). Order cards by modifiedAt desc. -->
    <div class="board">
      @for (state of states; track state) {
        <section class="column">
          <header>{{ labels[state] }}</header>
          <div class="cards"><!-- ticket cards --></div>
        </section>
      }
    </div>
  `,
  styles: [`
    .muted { color: var(--muted); }
    .board { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0.75rem; margin-top: 1rem; }
    .column { background: var(--panel); border: 1px solid var(--border); border-radius: 8px; min-height: 200px; }
    .column > header { padding: 0.5rem 0.75rem; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; border-bottom: 1px solid var(--border); }
    .cards { padding: 0.5rem; }
  `],
})
export class BoardComponent {
  readonly states = TICKET_STATES as readonly TicketState[];
  readonly labels = TICKET_STATE_LABELS;
}
