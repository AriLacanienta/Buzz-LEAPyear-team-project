import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '@environments/environment';

interface AuthApiResponse {
  token?: string;
  accessToken?: string;
  type?: string;
  username?: string;
  email?: string;
  roles?: string[];
}

/**
 * AuthService - Handles all authentication logic
 * 
 * Responsibilities:
 * - Call backend login/register endpoints
 * -switch to keep access token in memory only
 * - Check if user is logged in
 * - Logout user
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // Backend API endpoint for authentication
  private apiUrl = `${environment.apiUrl}/auth`;
  private accessToken: string | null = null;

  constructor(private http: HttpClient) { }

  /**
   * Login user with email and password
   * 
   * Makes POST request to /api/v1/auth/login
   * Returns Observable with response containing JWT token
   * 
   * @param email - User's email address
   * @param password - User's password
   * @returns Observable<any> - Response with token, username, email
   * 
   * Example response:
   * {
   *   "token": "eyJ...",
   *   "type": "Bearer",
   *   "username": "john",
   *   "email": "john@example.com"
   * }
   */
  login(email: string, password: string): Observable<AuthApiResponse> {
    return this.http.post<AuthApiResponse>(`${this.apiUrl}/login`, { email, password }, {
      withCredentials: true
    }).pipe(tap(response => this.storeAuthResponse(response)));
  }

  /**
   * Register new user
   * 
   * Makes POST request to /api/v1/auth/register
   * Returns Observable with response containing JWT token
   * User is automatically logged in after registration
   * 
   * @param username - Unique username
   * @param email - User's email address
   * @param firstName - User's first name
   * @param lastName - User's last name
   * @param password - User's password
   * @returns Observable<any> - Response with token, username, email
   */
  register(username: string, email: string, firstName: string,
           lastName: string, password: string): Observable<AuthApiResponse> {
    return this.http.post<AuthApiResponse>(`${this.apiUrl}/register`, {
      username, email, firstName, lastName, password
    }, {
      withCredentials: true
    }).pipe(tap(response => this.storeAuthResponse(response)));
  }

  //Exchanging refresh cookie for a new access token.
  refreshAccessToken(): Observable<AuthApiResponse> {
    return this.http.post<AuthApiResponse>(`${this.apiUrl}/refresh`, {}, {
      withCredentials: true
    }).pipe(tap(response => this.storeAuthResponse(response)));
  }

  restoreSession(): Observable<boolean> {
    if (this.isLoggedIn()) {
      return of(true);
    }

    return this.refreshAccessToken().pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  /**
  * Store access token in memory for the current tab session
   * 
   * Called after successful login/register
   * Token is persisted even after browser closes
   * 
   * @param token - JWT token from backend
   */
  storeToken(token: string): void {
    this.accessToken = token;
  }

  //Extracting token fields from auth response and persist them in memory
  setAuthSessionFromResponse(response: AuthApiResponse): void {
    const accessToken = response.accessToken || response.token;
    if (!accessToken) {
      return;
    }
    this.storeToken(accessToken);
  }

  storeAuthResponse(response: AuthApiResponse): void {
    this.setAuthSessionFromResponse(response);
    localStorage.setItem('auth_roles', JSON.stringify(response.roles ?? []));
  }

  /**
  * Retrieve access token from in-memory session state
   * 
   * Called when making authenticated requests
   * Returns null if token doesn't exist (user not logged in)
   * 
   * @returns JWT token string or null
   */
  getToken(): string | null {
    return this.getAccessToken();
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRoles(): string[] {
    const storedRoles = localStorage.getItem('auth_roles');
    if (!storedRoles) {
      return [];
    }

    try {
      const roles = JSON.parse(storedRoles);
      return Array.isArray(roles)
        ? roles.map(role => String(role).replace(/^ROLE_/, '').toUpperCase())
        : [];
    } catch {
      return [];
    }
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role.replace(/^ROLE_/, '').toUpperCase());
    //return this.getRoles().includes(role);
  }

  /**
   * Check if user is currently logged in
   * 
  * Checks whether an in-memory access token exists
   * Used by AuthGuard to protect routes
   * 
   * @returns true if token exists, false otherwise
   */
  isLoggedIn(): boolean {
    return this.getAccessToken() !== null;
  }

  /**
   * Logout user
   * 
  * Clears in-memory access token
   * Called when user clicks logout button or session expires
   * After logout, user must login again to access protected routes
   */
  logout(): void {
    const token = this.accessToken;
    const headers = token
      ? { Authorization: `Bearer ${token}` }
      : undefined;

    this.accessToken = null;
    localStorage.removeItem('auth_roles');

    this.http.post(`${this.apiUrl}/logout`, {}, {
      withCredentials: true,
      ...(headers ? { headers } : {})
    }).subscribe({
      error: () => {
        // Ignore logout API errors; client-side token is already cleared.
      }
    });
  }

  /**
   * Validate if stored token is valid
   * 
   * Makes GET request to /api/auth/validate
   * Useful for checking token validity on app startup
   * 
   * @returns Observable<any> - Response with valid status and username
   */
  validateToken(): Observable<any> {
    const token = this.getAccessToken();
    if (!token) {
      throw new Error('No token found');
    }
    return this.http.get(`${this.apiUrl}/validate`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
  }
}
