import { inject, Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { catchError, map } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService)
  const router = inject(Router);

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
      return true;
    }),
    catchError(error => {
      // Token validation failed (likely 401 Unauthorized)
      console.warn('Token validation failed, logging out', error);
      authService.logout();
      router.navigate(['/login']);
      return of(false);
    })
  );
};
