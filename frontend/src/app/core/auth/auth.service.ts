import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, map, of, shareReplay, tap, throwError } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { AuthResponse, AuthUser, LoginPayload, RegisterPayload } from './auth.models';

/**
 * Owns the authenticated session.
 *
 * The access token lives in a signal and nowhere else — not `localStorage`, not
 * `sessionStorage` — so a cross-site scripting bug has no stored credential to
 * read (ADR 0002). Persistence across reloads comes from the HttpOnly refresh
 * cookie, which this service exchanges for a new token on start-up.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly accessToken = signal<string | null>(null);
  private readonly currentUser = signal<AuthUser | null>(null);
  private readonly restoring = signal(true);

  /**
   * The in-flight refresh, shared so that several requests failing with 401 at
   * the same time trigger exactly one rotation. Without this, concurrent
   * refreshes would consume each other's tokens and trip the server's reuse
   * detection, logging the user out.
   */
  private refreshInFlight: Observable<string> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly isRestoringSession = this.restoring.asReadonly();

  token(): string | null {
    return this.accessToken();
  }

  register(payload: RegisterPayload): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>('/api/auth/register', payload, { withCredentials: true })
      .pipe(map((response) => this.acceptSession(response)));
  }

  login(payload: LoginPayload): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>('/api/auth/login', payload, { withCredentials: true })
      .pipe(map((response) => this.acceptSession(response)));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
      catchError(() => of(void 0)),
      // The local session is dropped regardless: a failed logout call must not
      // leave the interface believing the user is still signed in.
      finalize(() => this.clearSession()),
    );
  }

  /**
   * Attempts to restore a session from the refresh cookie. Runs once during
   * bootstrap, before any guard resolves, and never rejects — an anonymous
   * result is a valid outcome, not an error.
   */
  async restoreSession(): Promise<void> {
    try {
      await firstValueFrom(this.requestRefresh());
    } catch {
      this.clearSession();
    } finally {
      this.restoring.set(false);
    }
  }

  /** Used by the HTTP interceptor to recover from an expired access token. */
  refreshSession(): Observable<string> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.requestRefresh().pipe(
        finalize(() => (this.refreshInFlight = null)),
        shareReplay({ bufferSize: 1, refCount: true }),
      );
    }
    return this.refreshInFlight;
  }

  clearSession(): void {
    this.accessToken.set(null);
    this.currentUser.set(null);
  }

  /** Keeps the cached user in step after a profile update. */
  patchUser(user: AuthUser): void {
    this.currentUser.set(user);
  }

  private requestRefresh(): Observable<string> {
    return this.http.post<AuthResponse>('/api/auth/refresh', {}, { withCredentials: true }).pipe(
      tap((response) => this.acceptSession(response)),
      map((response) => response.accessToken),
      catchError((error) => {
        this.clearSession();
        return throwError(() => error);
      }),
    );
  }

  private acceptSession(response: AuthResponse): AuthUser {
    this.accessToken.set(response.accessToken);
    this.currentUser.set(response.user);
    return response.user;
  }
}
