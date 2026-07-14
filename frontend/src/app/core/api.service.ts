import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../environments/environment';
import { Activity, Comment, Epic, Team, Ticket, TicketInput, TicketState } from './models';

/**
 * Thin typed wrapper over the backend HTTP API. All create/update/delete goes
 * through here — the backend + RDBMS are the system of record.
 *
 * List/single responses are envelope-wrapped ({teams}/{team}/{epics}/{epic}); this
 * service unwraps them so callers get plain models.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // --- Teams ---
  listTeams(): Observable<Team[]> {
    return this.http
      .get<{ teams: Team[] }>(`${this.base}/teams`)
      .pipe(map((r) => r.teams));
  }
  createTeam(name: string): Observable<Team> {
    return this.http
      .post<{ team: Team }>(`${this.base}/teams`, { name })
      .pipe(map((r) => r.team));
  }
  renameTeam(id: string, name: string): Observable<Team> {
    return this.http
      .patch<{ team: Team }>(`${this.base}/teams/${id}`, { name })
      .pipe(map((r) => r.team));
  }
  deleteTeam(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/teams/${id}`);
  }

  // --- Epics ---
  listEpics(teamId: string): Observable<Epic[]> {
    return this.http
      .get<{ epics: Epic[] }>(`${this.base}/epics`, { params: { teamId } })
      .pipe(map((r) => r.epics));
  }
  createEpic(teamId: string, title: string, description?: string): Observable<Epic> {
    return this.http
      .post<{ epic: Epic }>(`${this.base}/epics`, { teamId, title, description })
      .pipe(map((r) => r.epic));
  }
  updateEpic(id: string, title: string, description?: string): Observable<Epic> {
    return this.http
      .patch<{ epic: Epic }>(`${this.base}/epics/${id}`, { title, description })
      .pipe(map((r) => r.epic));
  }
  deleteEpic(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/epics/${id}`);
  }

  // --- Tickets ---
  listTickets(teamId: string): Observable<Ticket[]> {
    return this.http
      .get<{ tickets: Ticket[] }>(`${this.base}/tickets`, { params: { teamId } })
      .pipe(map((r) => r.tickets));
  }
  getTicket(id: string): Observable<Ticket> {
    return this.http
      .get<{ ticket: Ticket }>(`${this.base}/tickets/${id}`)
      .pipe(map((r) => r.ticket));
  }
  createTicket(input: TicketInput): Observable<Ticket> {
    return this.http
      .post<{ ticket: Ticket }>(`${this.base}/tickets`, input)
      .pipe(map((r) => r.ticket));
  }
  updateTicket(id: string, input: TicketInput): Observable<Ticket> {
    return this.http
      .patch<{ ticket: Ticket }>(`${this.base}/tickets/${id}`, input)
      .pipe(map((r) => r.ticket));
  }
  updateTicketState(id: string, state: TicketState): Observable<Ticket> {
    return this.http
      .patch<{ ticket: Ticket }>(`${this.base}/tickets/${id}/state`, { state })
      .pipe(map((r) => r.ticket));
  }
  deleteTicket(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/tickets/${id}`);
  }

  // --- Comments ---
  listComments(ticketId: string): Observable<Comment[]> {
    return this.http
      .get<{ comments: Comment[] }>(`${this.base}/tickets/${ticketId}/comments`)
      .pipe(map((r) => r.comments));
  }
  addComment(ticketId: string, body: string): Observable<Comment> {
    return this.http
      .post<{ comment: Comment }>(`${this.base}/tickets/${ticketId}/comments`, { body })
      .pipe(map((r) => r.comment));
  }
  updateComment(ticketId: string, commentId: string, body: string): Observable<Comment> {
    return this.http
      .patch<{ comment: Comment }>(`${this.base}/tickets/${ticketId}/comments/${commentId}`, { body })
      .pipe(map((r) => r.comment));
  }
  deleteComment(ticketId: string, commentId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/tickets/${ticketId}/comments/${commentId}`);
  }

  // --- Activity ---
  listActivity(ticketId: string): Observable<Activity[]> {
    return this.http
      .get<{ activity: Activity[] }>(`${this.base}/tickets/${ticketId}/activity`)
      .pipe(map((r) => r.activity));
  }
}
