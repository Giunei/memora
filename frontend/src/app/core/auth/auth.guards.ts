import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Protects the application area.
 *
 * Safe to read the session synchronously: the app initialiser has already
 * awaited the refresh attempt, so by the time any route resolves the answer is
 * final rather than merely "not loaded yet".
 */
export const authenticatedGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { redirectTo: state.url } });
};

/** Keeps a signed-in user away from the landing, login and register screens. */
export const anonymousOnlyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated() ? router.createUrlTree(['/app/dashboard']) : true;
};
