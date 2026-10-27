import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRoles = route.data?.['roles'] as string[] | undefined;

  return authService.restoreSession().pipe(
    switchMap(restored => {
      if (!restored) {
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
