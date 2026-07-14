import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '../../core/api.service';
import { formatTimestamp } from '../../core/format';
import { Epic, Team } from '../../core/models';

type TeamsStatus = 'loading' | 'ready' | 'error';
type EpicsStatus = 'idle' | 'loading' | 'ready' | 'error';
type PanelMode = 'closed' | 'create' | 'edit';

const SELECTED_TEAM_KEY = 'ticketing.selectedTeamId';

@Component({
  selector: 'app-epics',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <section>
      <div class="page-toolbar">
        <div>
          <h1>Epics</h1>
          <div class="field field-inline">
            <label for="team-select">Team</label>
            <select id="team-select" class="toolbar-select"
                    [ngModel]="selectedTeamId()" [ngModelOptions]="{ standalone: true }"
                    (ngModelChange)="setSelectedTeam($event)"
                    [disabled]="teamsStatus() !== 'ready' || teams().length === 0">
              @for (t of teams(); track t.id) {
                <option [value]="t.id">{{ t.name }}</option>
              }
            </select>
          </div>
        </div>
        <button type="button" class="btn btn-inline" [disabled]="!selectedTeamId()" (click)="openCreate()">
          + Create epic
        </button>
      </div>

      @if (teamsStatus() === 'error') {
        <p class="error">Failed to load teams. Reload the page to retry.</p>
      }

      @if (teamsStatus() === 'ready' && teams().length === 0) {
        <p class="notice">
          No teams yet. Create one on the <a routerLink="/teams">Teams screen</a> before adding epics.
        </p>
      }

      @if (actionError()) {
        <p class="error">{{ actionError() }}</p>
      }

      @if (teams().length > 0) {
        <div [class]="panelOpen() ? 'epic-layout with-panel' : 'epic-layout'">
          <div>
            @if (epicsStatus() === 'loading') {
              <p class="notice">Loading epics…</p>
            }

            @if (epicsStatus() === 'error') {
              <div>
                <p class="error">{{ loadError() }}</p>
                <button type="button" class="btn btn-secondary btn-inline" (click)="reloadEpics()">Retry</button>
              </div>
            }

            @if (epicsStatus() === 'ready' && epics().length === 0) {
              <p class="notice">No epics for this team yet. Use “Create epic” to add one.</p>
            }

            @if (epicsStatus() === 'ready' && epics().length > 0) {
              <div class="table-card">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Tickets</th>
                      <th>Modified</th>
                      <th class="col-actions">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (epic of epics(); track epic.id) {
                      <tr>
                        <td>
                          <strong>{{ epic.title }}</strong>
                          @if (epic.description) {
                            <div class="subtitle">{{ epic.description }}</div>
                          }
                        </td>
                        <td>{{ epic.ticketCount }}</td>
                        <td>{{ fmt(epic.modifiedAt) }}</td>
                        <td>
                          <div class="row-actions">
                            <button type="button" class="btn btn-secondary btn-sm" (click)="openEdit(epic)">Edit</button>
                            <button type="button" class="btn btn-secondary btn-sm"
                                    [attr.aria-label]="'Delete ' + epic.title"
                                    [disabled]="epic.ticketCount > 0"
                                    [title]="epic.ticketCount > 0 ? 'Tickets reference this epic' : ''"
                                    (click)="onDelete(epic)">✕</button>
                          </div>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
                @if (anyReferenced()) {
                  <p class="help-text">Delete is disabled while tickets reference the epic.</p>
                }
              </div>
            }
          </div>

          @if (panelOpen()) {
            <form class="edit-panel" (ngSubmit)="onSave()">
              <h2>{{ panelMode() === 'create' ? 'Create epic' : 'Edit epic' }}</h2>
              <div class="field">
                <label for="epic-title">Title</label>
                <input id="epic-title" [(ngModel)]="title" [ngModelOptions]="{ standalone: true }" />
              </div>
              <div class="field">
                <label for="epic-description">Description (optional)</label>
                <textarea id="epic-description" [(ngModel)]="description"
                          [ngModelOptions]="{ standalone: true }"></textarea>
              </div>
              <div class="panel-actions">
                <button type="button" class="btn btn-secondary btn-inline" (click)="closePanel()">Cancel</button>
                <button type="submit" class="btn btn-inline" [disabled]="saving() || !title.trim()">
                  {{ saving() ? 'Saving…' : 'Save' }}
                </button>
              </div>
            </form>
          }
        </div>
      }
    </section>
  `,
})
export class EpicsComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly teamsStatus = signal<TeamsStatus>('loading');
  readonly teams = signal<Team[]>([]);
  readonly selectedTeamId = signal<string | null>(localStorage.getItem(SELECTED_TEAM_KEY));

  readonly epicsStatus = signal<EpicsStatus>('idle');
  readonly epics = signal<Epic[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly panelMode = signal<PanelMode>('closed');
  private readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  title = '';
  description = '';

  readonly fmt = formatTimestamp;

  ngOnInit(): void {
    void this.loadTeams();
  }

  panelOpen(): boolean {
    return this.panelMode() !== 'closed';
  }

  anyReferenced(): boolean {
    return this.epics().some((e) => e.ticketCount > 0);
  }

  async loadTeams(): Promise<void> {
    this.teamsStatus.set('loading');
    try {
      const teams = await firstValueFrom(this.api.listTeams());
      this.teams.set(teams);
      this.teamsStatus.set('ready');

      // Default the selection to the first team when nothing valid is selected.
      const current = this.selectedTeamId();
      const valid = current && teams.some((t) => t.id === current);
      const next = valid ? current : (teams[0]?.id ?? null);
      this.selectedTeamId.set(next);
      this.persist(next);
      if (next) {
        await this.loadEpics(next);
      }
    } catch {
      this.teamsStatus.set('error');
    }
  }

  setSelectedTeam(teamId: string): void {
    this.selectedTeamId.set(teamId);
    this.persist(teamId);
    this.closePanel();
    void this.loadEpics(teamId);
  }

  async loadEpics(teamId: string): Promise<void> {
    this.epicsStatus.set('loading');
    this.loadError.set(null);
    try {
      this.epics.set(await firstValueFrom(this.api.listEpics(teamId)));
      this.epicsStatus.set('ready');
    } catch (err) {
      this.loadError.set(this.msg(err, 'Failed to load epics'));
      this.epicsStatus.set('error');
    }
  }

  reloadEpics(): void {
    const id = this.selectedTeamId();
    if (id) {
      void this.loadEpics(id);
    }
  }

  openCreate(): void {
    if (!this.selectedTeamId()) {
      return;
    }
    this.actionError.set(null);
    this.title = '';
    this.description = '';
    this.editingId.set(null);
    this.panelMode.set('create');
  }

  openEdit(epic: Epic): void {
    this.actionError.set(null);
    this.editingId.set(epic.id);
    this.title = epic.title;
    this.description = epic.description ?? '';
    this.panelMode.set('edit');
  }

  closePanel(): void {
    this.panelMode.set('closed');
  }

  async onSave(): Promise<void> {
    const title = this.title.trim();
    if (!title || this.saving()) {
      return;
    }
    const description = this.description.trim() || undefined;
    this.saving.set(true);
    this.actionError.set(null);
    try {
      if (this.panelMode() === 'create') {
        const teamId = this.selectedTeamId();
        if (!teamId) {
          return;
        }
        await firstValueFrom(this.api.createEpic(teamId, title, description));
      } else {
        const id = this.editingId();
        if (!id) {
          return;
        }
        await firstValueFrom(this.api.updateEpic(id, title, description));
      }
      this.closePanel();
      this.reloadEpics();
    } catch (err) {
      this.actionError.set(this.msg(err, 'Could not save the epic'));
    } finally {
      this.saving.set(false);
    }
  }

  async onDelete(epic: Epic): Promise<void> {
    if (epic.ticketCount > 0) {
      return;
    }
    this.actionError.set(null);
    try {
      await firstValueFrom(this.api.deleteEpic(epic.id));
      this.reloadEpics();
    } catch (err) {
      this.actionError.set(this.msg(err, 'Could not delete the epic'));
    }
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
