import { inject, Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { catchError, map } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService)
  const router = inject(Router);
  const requiredRoles = route.data?.['roles'] as string[] | undefined;

  // First check if token exists locally
  if (!authService.isLoggedIn()) {
    console.warn('No token found, redirecting to login');
    return router.createUrlTree(['/login']);
  }

  // Token exists, now validate it with the backend
  // This will trigger a 401 if the token is expired
  return authService.validateToken().pipe(
    map(response => {
      // Token is valid
      console.log('Token validated successfully');
      if (authService.hasRole('ANALYST') && !authService.hasRole('MANAGER') && state.url !== '/analyst') {
        return router.createUrlTree(['/analyst']);
      }
      if (!requiredRoles?.length || requiredRoles.some(role => authService.hasRole(role))) {
        return true;
      }

      return router.createUrlTree(['/dashboard']);
    }),
    catchError(error => {
      // Token validation failed (likely 401 Unauthorized)
      console.warn('Token validation failed, logging out', error);
      authService.logout();
      return of(router.createUrlTree(['/login']));
    })
  );
};
