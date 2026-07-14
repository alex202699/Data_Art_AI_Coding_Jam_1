import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts unauthenticated', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
  });

  it('stores the token and user on successful login', async () => {
    const promise = service.login('user@example.com', 'password123');

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'jwt-123', user: { id: 'u1', email: 'user@example.com' } });

    await promise;

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('jwt-123');
    expect(service.user()?.email).toBe('user@example.com');
    expect(sessionStorage.getItem('ticketing.token')).toBe('jwt-123');
  });

  it('clears state on logout', async () => {
    service.setToken('jwt-123');
    const promise = service.logout();
    http.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    await promise;

    expect(service.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('ticketing.token')).toBeNull();
  });
});
