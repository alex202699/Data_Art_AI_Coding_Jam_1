import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '../../core/api.service';
import { VirtualCardListComponent } from './virtual-card-list.component';
import { formatRelative } from '../../core/format';
import {
  Epic,
  Team,
  Ticket,
  TICKET_STATES,
  TICKET_STATE_LABELS,
  TICKET_TYPES,
  TICKET_TYPE_LABELS,
  TicketState,
  TicketType,
} from '../../core/models';

type TeamsStatus = 'loading' | 'ready' | 'error';
type BoardStatus = 'idle' | 'loading' | 'ready' | 'error';

const SELECTED_TEAM_KEY = 'ticketing.selectedTeamId';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [FormsModule, RouterLink, VirtualCardListComponent],
  template: `
    <section>
      <div class="page-toolbar">
        <div>
          <div class="field field-inline">
            <label for="board-team">Team</label>
            <select id="board-team" class="toolbar-select"
                    [ngModel]="selectedTeamId()" [ngModelOptions]="{ standalone: true }"
                    (ngModelChange)="setSelectedTeam($event)"
                    [disabled]="teamsStatus() !== 'ready' || teams().length === 0">
              @for (t of teams(); track t.id) {
                <option [value]="t.id">{{ t.name }}</option>
              }
            </select>
          </div>
        </div>
        <button type="button" class="btn btn-inline" [disabled]="!selectedTeamId()" (click)="newTicket()">
          + New ticket
        </button>
      </div>

      @if (teamsStatus() === 'error') {
        <p class="error">Failed to load teams. Reload the page to retry.</p>
      }

      @if (teamsStatus() === 'ready' && teams().length === 0) {
        <p class="notice">
          No teams yet. Create one on the <a routerLink="/teams">Teams screen</a> before adding tickets.
        </p>
      }

      @if (selectedTeamId()) {
        <div class="board-filters">
          <div class="field field-inline board-search">
            <label for="filter-search">Search</label>
            <input id="filter-search" type="search" placeholder="Search title…"
                   [ngModel]="search()" [ngModelOptions]="{ standalone: true }"
                   (ngModelChange)="search.set($event)" />
          </div>
          <div class="field field-inline">
            <label for="filter-type">Type</label>
            <select id="filter-type" class="toolbar-select"
                    [ngModel]="typeFilter()" [ngModelOptions]="{ standalone: true }"
                    (ngModelChange)="typeFilter.set($event || null)">
              <option value="">All types</option>
              @for (v of types; track v) {
                <option [value]="v">{{ typeLabels[v] }}</option>
              }
            </select>
          </div>
          <div class="field field-inline">
            <label for="filter-epic">Epic</label>
            <select id="filter-epic" class="toolbar-select"
                    [ngModel]="epicFilter()" [ngModelOptions]="{ standalone: true }"
                    (ngModelChange)="epicFilter.set($event || null)">
              <option value="">All epics</option>
              @for (ep of epics(); track ep.id) {
                <option [value]="ep.id">{{ ep.title }}</option>
              }
            </select>
          </div>
          <button type="button" class="btn btn-secondary btn-sm" (click)="clearFilters()">Clear</button>
          <span class="filter-count">
            {{ tickets().length }} {{ tickets().length === 1 ? 'ticket' : 'tickets' }}
          </span>
        </div>

        @if (moveError()) {
          <p class="error">{{ moveError() }}</p>
        }

        @if (boardStatus() === 'loading') {
          <p class="notice">Loading board…</p>
        }

        @if (boardStatus() === 'error') {
          <div>
            <p class="error">{{ loadError() }}</p>
            <button type="button" class="btn btn-secondary btn-inline" (click)="reload()">Retry</button>
          </div>
        }

        @if (boardStatus() === 'ready') {
          <div class="board">
            @for (state of states; track state) {
              <div class="board-column" [class.drag-over]="dragOver() === state"
                   (dragover)="onDragOver($event, state)"
                   (dragleave)="onDragLeave(state)"
                   (drop)="onDrop(state)">
                <div class="board-column-header">
                  <span>{{ stateLabels[state] }}</span>
                  <span class="count">{{ grouped()[state].length }}</span>
                </div>
                <div class="board-column-body">
                  @if (grouped()[state].length === 0) {
                    <p class="board-empty">No tickets</p>
                  } @else {
                    <app-virtual-card-list
                      [items]="grouped()[state]"
                      [trackId]="trackTicketId"
                      [itemTemplate]="cardTpl" />
                  }
                </div>
              </div>
            }
          </div>
        }
      }
    </section>

    <!-- Shared card template (declared in the board so its bindings reach board methods). -->
    <ng-template #cardTpl let-t>
      <article class="ticket-card" [draggable]="true"
               (dragstart)="draggingId.set(t.id)" (dragend)="clearDrag()"
               (click)="open(t)" tabindex="0"
               (keydown.enter)="open(t)" (keydown.space)="open(t)">
        <span class="type-tag">{{ typeLabel(t) }}</span>
        <p class="ticket-card-title">{{ t.title }}</p>
        <div class="ticket-card-foot">
          <span class="ticket-card-epic">{{ t.epicTitle ? 'Epic: ' + t.epicTitle : 'No epic' }}</span>
          <span class="ticket-card-time">{{ rel(t.modifiedAt) }}</span>
        </div>
      </article>
    </ng-template>
  `,
})
export class BoardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly teams = signal<Team[]>([]);
  readonly teamsStatus = signal<TeamsStatus>('loading');
  readonly selectedTeamId = signal<string | null>(localStorage.getItem(SELECTED_TEAM_KEY));

  readonly tickets = signal<Ticket[]>([]);
  readonly epics = signal<Epic[]>([]);
  readonly boardStatus = signal<BoardStatus>('idle');
  readonly loadError = signal<string | null>(null);
  readonly moveError = signal<string | null>(null);

  readonly search = signal('');
  readonly typeFilter = signal<TicketType | null>(null);
  readonly epicFilter = signal<string | null>(null);

  readonly trackTicketId = (t: Ticket): string => t.id;

  // Card template's `t` is an untyped ng-template var; keep the label lookup typed here.
  typeLabel(t: Ticket): string {
    return TICKET_TYPE_LABELS[t.type];
  }

  readonly draggingId = signal<string | null>(null);
  readonly dragOver = signal<TicketState | null>(null);

  readonly states = TICKET_STATES;
  readonly types = TICKET_TYPES;
  readonly stateLabels = TICKET_STATE_LABELS;
  readonly typeLabels = TICKET_TYPE_LABELS;
  readonly rel = formatRelative;

  // Visible tickets grouped by column, filters AND-combined, newest-modified first.
  readonly grouped = computed<Record<TicketState, Ticket[]>>(() => {
    const needle = this.search().trim().toLowerCase();
    const type = this.typeFilter();
    const epicId = this.epicFilter();
    const groups = {
      new: [], ready_for_implementation: [], in_progress: [], ready_for_acceptance: [], done: [],
    } as Record<TicketState, Ticket[]>;
    for (const t of this.tickets()) {
      if (type && t.type !== type) continue;
      if (epicId && t.epicId !== epicId) continue;
      if (needle && !t.title.toLowerCase().includes(needle)) continue;
      groups[t.state].push(t);
    }
    for (const s of TICKET_STATES) {
      groups[s].sort((a, b) => new Date(b.modifiedAt).getTime() - new Date(a.modifiedAt).getTime());
    }
    return groups;
  });

  async ngOnInit(): Promise<void> {
    this.teamsStatus.set('loading');
    try {
      const teams = await firstValueFrom(this.api.listTeams());
      this.teams.set(teams);
      this.teamsStatus.set('ready');
      const current = this.selectedTeamId();
      const valid = current && teams.some((t) => t.id === current);
      const next = valid ? current : (teams[0]?.id ?? null);
      this.selectedTeamId.set(next);
      this.persist(next);
      if (next) {
        await this.loadBoard(next);
      }
    } catch {
      this.teamsStatus.set('error');
    }
  }

  setSelectedTeam(teamId: string): void {
    this.selectedTeamId.set(teamId);
    this.persist(teamId);
    this.clearFilters();
    void this.loadBoard(teamId);
  }

  reload(): void {
    const id = this.selectedTeamId();
    if (id) {
      void this.loadBoard(id);
    }
  }

  async loadBoard(teamId: string): Promise<void> {
    this.boardStatus.set('loading');
    this.moveError.set(null);
    this.loadError.set(null);
    try {
      const [t, e] = await Promise.all([
        firstValueFrom(this.api.listTickets(teamId)),
        firstValueFrom(this.api.listEpics(teamId)),
      ]);
      this.tickets.set(t);
      this.epics.set(e);
      this.boardStatus.set('ready');
    } catch (err) {
      this.loadError.set(this.msg(err, 'Failed to load the board'));
      this.boardStatus.set('error');
    }
  }

  clearFilters(): void {
    this.search.set('');
    this.typeFilter.set(null);
    this.epicFilter.set(null);
  }

  newTicket(): void {
    this.router.navigate(['/tickets/new']);
  }

  open(t: Ticket): void {
    this.router.navigate(['/tickets', t.id]);
  }

  onDragOver(event: DragEvent, state: TicketState): void {
    event.preventDefault();
    this.dragOver.set(state);
  }

  onDragLeave(state: TicketState): void {
    if (this.dragOver() === state) {
      this.dragOver.set(null);
    }
  }

  clearDrag(): void {
    this.draggingId.set(null);
    this.dragOver.set(null);
  }

  async onDrop(state: TicketState): Promise<void> {
    const id = this.draggingId();
    this.clearDrag();
    if (!id) {
      return;
    }
    const current = this.tickets().find((t) => t.id === id);
    if (!current || current.state === state) {
      return;
    }
    const previous = current.state;
    this.moveError.set(null);
    // Optimistically move, then persist; roll back on failure.
    this.patchTicket(id, { state, modifiedAt: new Date().toISOString() });
    try {
      const updated = await firstValueFrom(this.api.updateTicketState(id, state));
      this.tickets.update((list) => list.map((t) => (t.id === id ? updated : t)));
    } catch (err) {
      this.patchTicket(id, { state: previous });
      this.moveError.set(this.msg(err, 'Could not move the ticket. It was returned to its column.'));
    }
  }

  private patchTicket(id: string, patch: Partial<Ticket>): void {
    this.tickets.update((list) => list.map((t) => (t.id === id ? { ...t, ...patch } : t)));
  }

  private persist(teamId: string | null): void {
    if (teamId) {
      localStorage.setItem(SELECTED_TEAM_KEY, teamId);
    } else {
      localStorage.removeItem(SELECTED_TEAM_KEY);
    }
  }

  private msg(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error.error === 'string') {
      return err.error.error;
    }
    return fallback;
  }
}
