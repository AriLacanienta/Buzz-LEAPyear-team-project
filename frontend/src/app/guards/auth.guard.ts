import { inject, Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService)
  const router = inject(Router);
  const requiredRoles = route.data?.['roles'] as string[] | undefined;

  // Recover the in-memory access token after a reload before validating it.
  return authService.restoreSession().pipe(
    switchMap(restored => {
      if (!restored) {
        return of(router.createUrlTree(['/login']));
      }

      return authService.validateToken().pipe(
        map(() => {
          if (!requiredRoles?.length || requiredRoles.some(role => authService.hasRole(role))) {
            return true;
          }

          return router.createUrlTree(['/dashboard']);
        })
      );
    }),
    catchError(error => {
      // Token validation failed (likely 401 Unauthorized)
      console.warn('Token validation failed, logging out', error);
      authService.logout();
      return of(router.createUrlTree(['/login']));
    })
  );
};
