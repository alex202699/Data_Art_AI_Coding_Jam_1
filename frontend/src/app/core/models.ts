// Canonical API enum values and their human-readable labels.

export const TICKET_TYPES = ['bug', 'feature', 'fix'] as const;
export type TicketType = (typeof TICKET_TYPES)[number];

export const TICKET_TYPE_LABELS: Record<TicketType, string> = {
  bug: 'Bug',
  feature: 'Feature',
  fix: 'Fix',
};

// Workflow order — the five board columns, left to right.
export const TICKET_STATES = [
  'new',
  'ready_for_implementation',
  'in_progress',
  'ready_for_acceptance',
  'done',
] as const;
export type TicketState = (typeof TICKET_STATES)[number];

export const TICKET_STATE_LABELS: Record<TicketState, string> = {
  new: 'New',
  ready_for_implementation: 'Ready for Implementation',
  in_progress: 'In Progress',
  ready_for_acceptance: 'Ready for Acceptance',
  done: 'Done',
};

export interface Team {
  id: string;
  name: string;
  ticketCount: number;
  epicCount: number;
  createdAt: string;
  modifiedAt: string;
}

export interface Epic {
  id: string;
  teamId: string;
  title: string;
  description: string | null;
  ticketCount: number;
  createdAt: string;
  modifiedAt: string;
}

export interface Ticket {
  id: string;
  teamId: string;
  epicId?: string | null;
  type: TicketType;
  state: TicketState;
  title: string;
  body: string;
  createdBy: string;
  createdAt: string;
  modifiedAt: string;
}

export interface Comment {
  id: string;
  ticketId: string;
  author: string;
  body: string;
  createdAt: string;
}

// --- Auth ---
export interface User {
  id: string;
  email: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface MessageResponse {
  message: string;
}

export interface VerifyResponse {
  status: 'verified' | 'invalid';
}
