import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

/*
this file automatically adds JWT token to all HTTP requests
This interceptor runs on EVERY HTTP request made by the app
It extracts the JWT token from localStorage and adds it to the Authorization header
So instead of manually adding token to each request, it's done automatically
 
 Added functionality to handle 401 Unauthorized responses by
 clearing the stored token and redirecting to login page
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) { }

  /**
   * Intercept method called for every HTTP request
   * 
   * @param req The outgoing HTTP request
   * @param next HttpHandler to pass request along the chain
   * @returns Observable<HttpEvent<any>> The response
   */
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    
    //Gets JWT token from localStorage
    //authService.getToken() returns the token or null if not logged in
    const token = this.authService.getAccessToken();

    // If token exists, add it to the Authorization header
    if (token) {
      // Clone the request (HttpRequest is immutable, can't modify directly)
      // Add Authorization header with Bearer token
      req = req.clone({
        setHeaders: {
          'Authorization': `Bearer ${token}`
        }
      });
    }

    //passes the request to the next interceptor in the chain
    //allows the request to continue to the server
    return next.handle(req).pipe(
      catchError(error => {
        const isRefreshCall = req.url.includes('/auth/refresh');

        //try refresh flow once for non refresh calls
        if (error.status === 401 && !isRefreshCall) {
          //access token is expired recovering with refresh token
          const refreshToken = this.authService.getRefreshToken();
          if (!refreshToken) {
            this.authService.logout();
            this.router.navigate(['/login']);
            return throwError(() => error);
          }

          return this.authService.refreshAccessToken(refreshToken).pipe(
            switchMap((authResponse) => {
              this.authService.setAuthSessionFromResponse(authResponse);
              const updatedAccessToken = this.authService.getAccessToken();

              if (!updatedAccessToken) {
                this.authService.logout();
                this.router.navigate(['/login']);
                return throwError(() => error);
              }

              const retryRequest = req.clone({
                setHeaders: {
                  'Authorization': `Bearer ${updatedAccessToken}`
                }
              });
              //retrying the original request once with the new access token
              return next.handle(retryRequest);
            }),
            catchError(refreshError => {
              this.authService.logout();
              this.router.navigate(['/login']);
              return throwError(() => refreshError);
            })
          );
        }
        return throwError(() => error);
      })
    );
  }
}
