import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../environments/environment';
import { Comment, Epic, Team, Ticket } from './models';

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

  // --- Tickets (endpoints land with the board feature) ---
  listTickets(teamId: string): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.base}/teams/${teamId}/tickets`);
  }
  getTicket(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/tickets/${id}`);
  }
  createTicket(payload: Partial<Ticket>): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/tickets`, payload);
  }
  updateTicket(id: string, payload: Partial<Ticket>): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.base}/tickets/${id}`, payload);
  }
  deleteTicket(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/tickets/${id}`);
  }

  // --- Comments ---
  listComments(ticketId: string): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.base}/tickets/${ticketId}/comments`);
  }
  addComment(ticketId: string, body: string): Observable<Comment> {
    return this.http.post<Comment>(`${this.base}/tickets/${ticketId}/comments`, { body });
  }
}
