import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Session endpoints authenticate with the cookie, never with a bearer token. */
function isSessionEndpoint(url: string): boolean {
  return url.startsWith('/api/auth/');
}

/**
 * Attaches the bearer token and transparently recovers from an expired one.
 *
 * On a 401 the request is retried once with a freshly rotated token. If the
 * refresh itself fails the session is genuinely over, so the local state is
 * dropped and the user is sent to the login screen with a `redirectTo` hint.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (isSessionEndpoint(request.url)) {
    return next(request);
  }

  return next(withBearer(request, auth.token())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refreshSession().pipe(
        switchMap((token) => next(withBearer(request, token))),
        catchError(() => {
          auth.clearSession();
          void router.navigate(['/login'], {
            queryParams: { redirectTo: router.url },
          });
          return throwError(() => error);
        }),
      );
    }),
  );
};

function withBearer<T>(request: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}
