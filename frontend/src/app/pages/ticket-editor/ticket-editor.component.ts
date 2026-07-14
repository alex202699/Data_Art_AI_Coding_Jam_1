import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '../../core/api.service';
import { formatCommentTime, formatDateTimeUTC } from '../../core/format';
import {
  Comment,
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

const SELECTED_TEAM_KEY = 'ticketing.selectedTeamId';

@Component({
  selector: 'app-ticket-editor',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="ticket-panel">
      <a class="back-link" routerLink="/">← Back to {{ backTeamName() }}</a>

      @if (loading()) {
        <p class="notice">Loading ticket…</p>
      } @else if (loadError()) {
        <p class="error">{{ loadError() }}</p>
      } @else {
        <div class="ticket-toolbar">
          <div>
            @if (mode === 'edit') {
              <p class="ticket-meta">
                #{{ shortId() }} · Created by {{ createdByEmail() }}
                · Created {{ fmtUtc(createdAt()) }} · Modified {{ fmtUtc(modifiedAt()) }}
              </p>
            } @else {
              <p class="ticket-meta">New ticket</p>
            }
            <h1 class="ticket-heading">{{ mode === 'create' ? 'Create ticket' : (title || 'Ticket') }}</h1>
          </div>
          <div class="ticket-toolbar-actions">
            @if (mode === 'edit') {
              <button type="button" class="btn btn-secondary btn-inline" (click)="remove()">Delete</button>
            }
            <button form="ticket-form" type="submit" class="btn btn-inline"
                    [disabled]="saving() || !title.trim() || !body.trim()">
              {{ saving() ? 'Saving…' : (mode === 'create' ? 'Create' : 'Save') }}
            </button>
          </div>
        </div>

        @if (formError()) {
          <p class="error">{{ formError() }}</p>
        }

        <div class="ticket-detail" [class.with-comments]="mode === 'edit'">
          <form id="ticket-form" class="ticket-form" (ngSubmit)="save()">
            <div class="form-grid" [class.form-grid-3]="mode === 'edit'">
              <div class="field">
                <label for="ticket-team">Team</label>
                <select id="ticket-team" [ngModel]="teamId" [ngModelOptions]="{ standalone: true }"
                        (ngModelChange)="onTeamChange($event)">
                  @for (t of teams(); track t.id) {
                    <option [value]="t.id">{{ t.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="ticket-type">Type</label>
                <select id="ticket-type" [(ngModel)]="type" [ngModelOptions]="{ standalone: true }">
                  @for (v of types; track v) {
                    <option [value]="v">{{ typeLabels[v] }}</option>
                  }
                </select>
              </div>
              @if (mode === 'edit') {
                <div class="field">
                  <label for="ticket-state">State</label>
                  <select id="ticket-state" [(ngModel)]="state" [ngModelOptions]="{ standalone: true }">
                    @for (s of states; track s) {
                      <option [value]="s">{{ stateLabels[s] }}</option>
                    }
                  </select>
                </div>
              }
            </div>

            <div class="field">
              <label for="ticket-epic">Epic (optional)</label>
              <select id="ticket-epic" [(ngModel)]="epicId" [ngModelOptions]="{ standalone: true }">
                <option value="">— None —</option>
                @for (ep of epics(); track ep.id) {
                  <option [value]="ep.id">{{ ep.title }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="ticket-title">Title</label>
              <input id="ticket-title" [(ngModel)]="title" [ngModelOptions]="{ standalone: true }" />
            </div>
            <div class="field">
              <label for="ticket-body">Body</label>
              <textarea id="ticket-body" class="ticket-body-input"
                        [(ngModel)]="body" [ngModelOptions]="{ standalone: true }"></textarea>
            </div>
          </form>

          @if (mode === 'edit') {
            <div class="ticket-side">
              <aside class="comments">
                <div class="comments-header">
                  <h2>Comments</h2>
                  <span class="count">{{ comments().length }}</span>
                </div>
                @if (comments().length === 0) {
                  <p class="notice">No comments yet.</p>
                } @else {
                  <ul class="comment-list">
                    @for (c of comments(); track c.id) {
                      <li class="comment">
                        <div class="comment-meta">
                          <strong>{{ c.authorEmail }}</strong>
                          <span>{{ commentTime(c.createdAt) }}</span>
                        </div>
                        <p class="comment-body">{{ c.body }}</p>
                      </li>
                    }
                  </ul>
                }
                <form class="comment-form" (ngSubmit)="addComment()">
                  <label for="new-comment">Add comment</label>
                  <textarea id="new-comment" placeholder="Write a comment…"
                            [(ngModel)]="commentBody" [ngModelOptions]="{ standalone: true }"></textarea>
                  @if (commentError()) {
                    <p class="error">{{ commentError() }}</p>
                  }
                  <div class="panel-actions">
                    <button type="submit" class="btn btn-inline" [disabled]="posting() || !commentBody.trim()">
                      {{ posting() ? 'Posting…' : 'Post comment' }}
                    </button>
                  </div>
                </form>
              </aside>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class TicketEditorComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  mode: 'create' | 'edit' = 'create';
  private ticketId: string | null = null;

  readonly teams = signal<Team[]>([]);
  readonly epics = signal<Epic[]>([]);
  readonly comments = signal<Comment[]>([]);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly posting = signal(false);
  readonly commentError = signal<string | null>(null);

  // Loaded ticket metadata (edit mode).
  private readonly loaded = signal<Ticket | null>(null);

  // Form fields.
  teamId = '';
  type: TicketType = 'bug';
  state: TicketState = 'new';
  epicId = '';
  title = '';
  body = '';
  commentBody = '';

  readonly types = TICKET_TYPES;
  readonly states = TICKET_STATES;
  readonly typeLabels = TICKET_TYPE_LABELS;
  readonly stateLabels = TICKET_STATE_LABELS;
  readonly fmtUtc = formatDateTimeUTC;
  readonly commentTime = formatCommentTime;

  shortId(): string {
    return this.ticketId ? this.ticketId.slice(0, 8) : '';
  }
  createdByEmail(): string {
    return this.loaded()?.createdByEmail ?? '';
  }
  createdAt(): string {
    return this.loaded()?.createdAt ?? '';
  }
  modifiedAt(): string {
    return this.loaded()?.modifiedAt ?? '';
  }
  backTeamName(): string {
    const t = this.teams().find((x) => x.id === this.teamId);
    return t ? t.name : 'board';
  }

  async ngOnInit(): Promise<void> {
    this.mode = this.route.snapshot.data['mode'] === 'edit' ? 'edit' : 'create';
    this.ticketId = this.route.snapshot.paramMap.get('id');
    try {
      const teams = await firstValueFrom(this.api.listTeams());
      this.teams.set(teams);

      if (this.mode === 'edit' && this.ticketId) {
        const [ticket, comments] = await Promise.all([
          firstValueFrom(this.api.getTicket(this.ticketId)),
          firstValueFrom(this.api.listComments(this.ticketId)),
        ]);
        this.loaded.set(ticket);
        this.teamId = ticket.teamId;
        this.type = ticket.type;
        this.state = ticket.state;
        this.epicId = ticket.epicId ?? '';
        this.title = ticket.title;
        this.body = ticket.body;
        this.comments.set(comments);
      } else {
        const stored = localStorage.getItem(SELECTED_TEAM_KEY);
        const valid = stored && teams.some((t) => t.id === stored);
        this.teamId = valid ? (stored as string) : (teams[0]?.id ?? '');
      }

      if (this.teamId) {
        this.epics.set(await firstValueFrom(this.api.listEpics(this.teamId)));
      }
    } catch (err) {
      this.loadError.set(this.msg(err, 'Failed to load the ticket'));
    } finally {
      this.loading.set(false);
    }
  }

  async onTeamChange(nextTeamId: string): Promise<void> {
    this.teamId = nextTeamId;
    // Clear the epic unless the loaded ticket already belongs to this team.
    const loaded = this.loaded();
    if (!loaded || loaded.teamId !== nextTeamId) {
      this.epicId = '';
    }
    try {
      this.epics.set(await firstValueFrom(this.api.listEpics(nextTeamId)));
    } catch {
      this.epics.set([]);
    }
  }

  async save(): Promise<void> {
    const title = this.title.trim();
    const body = this.body.trim();
    if (!title || !body) {
      this.formError.set('Title and body are required');
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    const epicId = this.epicId || null;
    try {
      if (this.mode === 'create') {
        await firstValueFrom(
          this.api.createTicket({ teamId: this.teamId, epicId, type: this.type, title, body }),
        );
      } else if (this.ticketId) {
        await firstValueFrom(
          this.api.updateTicket(this.ticketId, {
            teamId: this.teamId, epicId, type: this.type, state: this.state, title, body,
          }),
        );
      }
      this.router.navigate(['/']);
    } catch (err) {
      this.formError.set(this.msg(err, 'Could not save the ticket'));
    } finally {
      this.saving.set(false);
    }
  }

  async remove(): Promise<void> {
    if (!this.ticketId) {
      return;
    }
    if (!confirm('Delete this ticket and its comments? This cannot be undone.')) {
      return;
    }
    try {
      await firstValueFrom(this.api.deleteTicket(this.ticketId));
      this.router.navigate(['/']);
    } catch (err) {
      this.formError.set(this.msg(err, 'Could not delete the ticket'));
    }
  }

  async addComment(): Promise<void> {
    const body = this.commentBody.trim();
    if (!body || !this.ticketId || this.posting()) {
      return;
    }
    this.posting.set(true);
    this.commentError.set(null);
    try {
      const created = await firstValueFrom(this.api.addComment(this.ticketId, body));
      this.comments.update((list) => [...list, created]);
      this.commentBody = '';
    } catch (err) {
      this.commentError.set(this.msg(err, 'Could not add the comment'));
    } finally {
      this.posting.set(false);
    }
  }

  private msg(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error.error === 'string') {
      return err.error.error;
    }
    return fallback;
  }
}
