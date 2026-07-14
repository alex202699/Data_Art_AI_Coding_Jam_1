import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ApiService } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('unwraps the teams envelope on list', () => {
    let result: unknown;
    api.listTeams().subscribe((r) => (result = r));
    const req = http.expectOne('/api/teams');
    expect(req.request.method).toBe('GET');
    req.flush({ teams: [{ id: 't1', name: 'A', ticketCount: 0, epicCount: 0 }] });
    expect((result as unknown[]).length).toBe(1);
  });

  it('creates a team and unwraps {team}', () => {
    let created: any;
    api.createTeam('Payments').subscribe((t) => (created = t));
    const req = http.expectOne('/api/teams');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Payments' });
    req.flush({ team: { id: 't1', name: 'Payments' } });
    expect(created.id).toBe('t1');
  });

  it('lists epics with a teamId query param', () => {
    api.listEpics('team-9').subscribe();
    const req = http.expectOne((r) => r.url === '/api/epics' && r.params.get('teamId') === 'team-9');
    expect(req.request.method).toBe('GET');
    req.flush({ epics: [] });
  });

  it('lists tickets and unwraps {tickets}', () => {
    let result: unknown[] = [];
    api.listTickets('team-1').subscribe((r) => (result = r));
    const req = http.expectOne((r) => r.url === '/api/tickets' && r.params.get('teamId') === 'team-1');
    req.flush({ tickets: [{ id: 'k1' }, { id: 'k2' }] });
    expect(result.length).toBe(2);
  });

  it('creates a ticket via POST /api/tickets', () => {
    api.createTicket({ teamId: 't1', epicId: null, type: 'bug', title: 'T', body: 'B' }).subscribe();
    const req = http.expectOne('/api/tickets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.type).toBe('bug');
    req.flush({ ticket: { id: 'k1' } });
  });

  it('changes ticket state via PATCH /api/tickets/:id/state', () => {
    let updated: any;
    api.updateTicketState('k1', 'in_progress').subscribe((t) => (updated = t));
    const req = http.expectOne('/api/tickets/k1/state');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ state: 'in_progress' });
    req.flush({ ticket: { id: 'k1', state: 'in_progress' } });
    expect(updated.state).toBe('in_progress');
  });

  it('adds and lists comments under a ticket', () => {
    api.addComment('k1', 'Hello').subscribe();
    const post = http.expectOne('/api/tickets/k1/comments');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ body: 'Hello' });
    post.flush({ comment: { id: 'c1', body: 'Hello' } });

    let comments: unknown[] = [];
    api.listComments('k1').subscribe((r) => (comments = r));
    const getReq = http.expectOne('/api/tickets/k1/comments');
    expect(getReq.request.method).toBe('GET');
    getReq.flush({ comments: [{ id: 'c1' }] });
    expect(comments.length).toBe(1);
  });
});
