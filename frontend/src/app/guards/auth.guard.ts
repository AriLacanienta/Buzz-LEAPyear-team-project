import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRoles = route.data?.['roles'] as string[] | undefined;

  // Access tokens are memory-only; restore one from the refresh cookie after reload.
  const sessionReady$: Observable<boolean> = authService.isLoggedIn()
    ? of(true)
    : authService.refreshAccessToken().pipe(
        map(response => {
          authService.setAuthSessionFromResponse(response);
          return authService.isLoggedIn();
        }),
        catchError(() => of(false))
      );

  return sessionReady$.pipe(
    switchMap(sessionReady => {
      if (!sessionReady) {
        return of(router.createUrlTree(['/login']));
      }

      return authService.validateToken().pipe(
        map(() => {
          // Analyst-only users should stay within their analyst workspace.
          if (
            authService.hasRole('ANALYST') &&
            !authService.hasRole('MANAGER') &&
            state.url !== '/analyst'
          ) {
            return router.createUrlTree(['/analyst']);
          }

          if (!requiredRoles?.length || requiredRoles.some(role => authService.hasRole(role))) {
            return true;
          }

          return router.createUrlTree(['/dashboard']);
        })
      );
    }),
    catchError(error => {
      console.warn('Token validation failed, logging out', error);
      authService.logout();
      return of(router.createUrlTree(['/login']));
    })
  );
};
