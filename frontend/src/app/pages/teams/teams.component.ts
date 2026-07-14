import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '../../core/api.service';
import { formatTimestamp } from '../../core/format';
import { Team } from '../../core/models';

type Status = 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-teams',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="popover-anchor">
      <div class="page-toolbar">
        <div>
          <h1>Teams</h1>
          <p class="help-text">All verified users can view and manage all teams.</p>
        </div>
        <button type="button" class="btn btn-inline" (click)="openCreate()">+ Create team</button>
      </div>

      @if (actionError()) {
        <p class="error">{{ actionError() }}</p>
      }

      @if (status() === 'loading') {
        <p class="notice">Loading teams…</p>
      }

      @if (status() === 'error') {
        <div>
          <p class="error">{{ loadError() }}</p>
          <button type="button" class="btn btn-secondary btn-inline" (click)="load()">Retry</button>
        </div>
      }

      @if (status() === 'ready' && teams().length === 0) {
        <p class="notice">No teams yet. Use “Create team” to add your first one.</p>
      }

      @if (status() === 'ready' && teams().length > 0) {
        <div class="table-card">
          <table class="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Tickets</th>
                <th>Epics</th>
                <th>Modified</th>
                <th class="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (team of teams(); track team.id) {
                <tr>
                  <td>
                    @if (editId() === team.id) {
                      <input aria-label="Team name" [(ngModel)]="editName"
                             [ngModelOptions]="{ standalone: true }" />
                    } @else {
                      <strong>{{ team.name }}</strong>
                    }
                  </td>
                  <td>{{ team.ticketCount }}</td>
                  <td>{{ team.epicCount }}</td>
                  <td>{{ fmt(team.modifiedAt) }}</td>
                  <td>
                    <div class="row-actions">
                      @if (editId() === team.id) {
                        <button type="button" class="btn btn-sm" (click)="saveEdit(team)">Save</button>
                        <button type="button" class="btn btn-secondary btn-sm" (click)="cancelEdit()">Cancel</button>
                      } @else {
                        <button type="button" class="btn btn-secondary btn-sm" (click)="startEdit(team)">Edit</button>
                        <button type="button" class="btn btn-secondary btn-sm"
                                [disabled]="referenced(team)"
                                [title]="referenced(team) ? 'This team contains tickets or epics' : ''"
                                (click)="onDelete(team)">Delete</button>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
          @if (anyReferenced()) {
            <p class="help-text">Delete is disabled while a team contains tickets or epics.</p>
          }
        </div>
      }

      @if (showCreate()) {
        <div class="create-popover" role="dialog" aria-label="Create team">
          <div class="create-popover-head">
            <h2>Create team</h2>
            <button type="button" class="create-popover-close" aria-label="Close" (click)="closeCreate()">✕</button>
          </div>
          <form (ngSubmit)="onCreate()">
            <div class="field">
              <label for="team-name">Team name</label>
              <div class="create-row">
                <input id="team-name" placeholder="e.g. Platform Engineering"
                       [(ngModel)]="newName" [ngModelOptions]="{ standalone: true }" />
                <button type="submit" class="btn btn-inline"
                        [disabled]="creating() || !newName.trim()">
                  {{ creating() ? 'Creating…' : 'Create' }}
                </button>
              </div>
            </div>
            @if (createError()) {
              <p class="error">{{ createError() }}</p>
            }
          </form>
        </div>
      }
    </section>
  `,
})
export class TeamsComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly status = signal<Status>('loading');
  readonly teams = signal<Team[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly editId = signal<string | null>(null);
  editName = '';

  readonly showCreate = signal(false);
  readonly creating = signal(false);
  readonly createError = signal<string | null>(null);
  newName = '';

  readonly fmt = formatTimestamp;

  ngOnInit(): void {
    void this.load();
  }

  referenced(team: Team): boolean {
    return team.ticketCount + team.epicCount > 0;
  }

  anyReferenced(): boolean {
    return this.teams().some((t) => this.referenced(t));
  }

  async load(): Promise<void> {
    this.status.set('loading');
    this.loadError.set(null);
    try {
      this.teams.set(await firstValueFrom(this.api.listTeams()));
      this.status.set('ready');
    } catch (err) {
      this.loadError.set(this.msg(err, 'Failed to load teams'));
      this.status.set('error');
    }
  }

  openCreate(): void {
    this.newName = '';
    this.createError.set(null);
    this.showCreate.set(true);
  }

  closeCreate(): void {
    this.showCreate.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.showCreate()) {
      this.closeCreate();
    }
  }

  async onCreate(): Promise<void> {
    const name = this.newName.trim();
    if (!name || this.creating()) {
      return;
    }
    this.creating.set(true);
    this.createError.set(null);
    try {
      await firstValueFrom(this.api.createTeam(name));
      this.closeCreate();
      await this.load();
    } catch (err) {
      this.createError.set(this.msg(err, 'Could not create the team'));
    } finally {
      this.creating.set(false);
    }
  }

  startEdit(team: Team): void {
    this.actionError.set(null);
    this.editId.set(team.id);
    this.editName = team.name;
  }

  cancelEdit(): void {
    this.editId.set(null);
  }

  async saveEdit(team: Team): Promise<void> {
    const name = this.editName.trim();
    if (!name) {
      return;
    }
    try {
      await firstValueFrom(this.api.renameTeam(team.id, name));
      this.editId.set(null);
      await this.load();
    } catch (err) {
      this.actionError.set(this.msg(err, 'Could not rename the team'));
    }
  }

  async onDelete(team: Team): Promise<void> {
    if (this.referenced(team)) {
      return;
    }
    this.actionError.set(null);
    try {
      await firstValueFrom(this.api.deleteTeam(team.id));
      await this.load();
    } catch (err) {
      this.actionError.set(this.msg(err, 'Could not delete the team'));
    }
  }

  private msg(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error.error === 'string') {
      return err.error.error;
    }
    return fallback;
  }
}
