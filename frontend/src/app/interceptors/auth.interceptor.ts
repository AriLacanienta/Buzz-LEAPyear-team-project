import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

/*
this file automatically adds JWT token to all HTTP requests
This interceptor runs on EVERY HTTP request made by the app
It extracts the JWT token from localStorage and adds it to the Authorization header
 So instead of manually adding token to each request, it's done automatically
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) { }

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
    const token = this.authService.getToken();

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

    //Passes the request to the next interceptor in the chain
    //allows the request to continue to the server
    return next.handle(req);
  }
}
