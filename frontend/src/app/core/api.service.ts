import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Comment, Epic, Team, Ticket } from './models';

/**
 * Thin typed wrapper over the backend HTTP API. All create/update/delete goes
 * through here — the backend + RDBMS are the system of record.
 *
 * Endpoints below match the intended API contract; fill in the backend to serve them.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // --- Teams ---
  listTeams(): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.base}/teams`);
  }
  createTeam(name: string): Observable<Team> {
    return this.http.post<Team>(`${this.base}/teams`, { name });
  }
  renameTeam(id: string, name: string): Observable<Team> {
    return this.http.put<Team>(`${this.base}/teams/${id}`, { name });
  }
  deleteTeam(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/teams/${id}`);
  }

  // --- Epics ---
  listEpics(teamId: string): Observable<Epic[]> {
    return this.http.get<Epic[]>(`${this.base}/teams/${teamId}/epics`);
  }
  createEpic(teamId: string, title: string, description?: string): Observable<Epic> {
    return this.http.post<Epic>(`${this.base}/teams/${teamId}/epics`, { title, description });
  }
  updateEpic(id: string, title: string, description?: string): Observable<Epic> {
    return this.http.put<Epic>(`${this.base}/epics/${id}`, { title, description });
  }
  deleteEpic(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/epics/${id}`);
  }

  // --- Tickets ---
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
