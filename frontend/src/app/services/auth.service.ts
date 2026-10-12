import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

interface AuthApiResponse {
  token?: string;
  accessToken?: string;
  refreshToken?: string;
  type?: string;
  username?: string;
  email?: string;
}

/**
 * AuthService - Handles all authentication logic
 * 
 * Responsibilities:
 * - Call backend login/register endpoints
 * - Store/retrieve JWT token from localStorage
 * - Check if user is logged in
 * - Logout user
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // Backend API endpoint for authentication
  private apiUrl = `${environment.apiUrl}/auth`;
  private readonly accessTokenKey = 'jwt_access_token';
  private readonly refreshTokenKey = 'jwt_refresh_token';
  private readonly legacyTokenKey = 'jwt_token';

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
    return this.http.post(`${this.apiUrl}/login`, { email, password });
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
    return this.http.post(`${this.apiUrl}/register`, {
      username, email, firstName, lastName, password
    });
  }

  //Exchanging refresh token for a new access/refresh token pair.
  refreshAccessToken(refreshToken: string): Observable<AuthApiResponse> {
    return this.http.post<AuthApiResponse>(`${this.apiUrl}/refresh`, { refreshToken });
  }

  /**
   * Storing JWT token in browser's localStorage
   * 
   * Called after successful login/register
   * Token is persisted even after browser closes
   * 
   * @param token - JWT token from backend
   */
  storeToken(token: string): void {
    this.storeTokens(token, this.getRefreshToken());
  }
  //storing refresh and access tokens in localStorage
  storeTokens(accessToken: string, refreshToken: string | null): void {
    localStorage.setItem(this.accessTokenKey, accessToken);
    //keeping original key so existing code paths continue to work
    localStorage.setItem(this.legacyTokenKey, accessToken);

    if (refreshToken) {
      localStorage.setItem(this.refreshTokenKey, refreshToken);
    } else {
      localStorage.removeItem(this.refreshTokenKey);
    }
  }

  //Extracting token fields from auth response and persist them
  setAuthSessionFromResponse(response: AuthApiResponse): void {
    const accessToken = response.accessToken || response.token;
    if (!accessToken) {
      return;
    }
    this.storeTokens(accessToken, response.refreshToken || this.getRefreshToken());
  }

  /**
   * Retrieve JWT token from localStorage
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
    return localStorage.getItem(this.accessTokenKey) || localStorage.getItem(this.legacyTokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  /**
   * Check if user is currently logged in
   * 
   * Simply checks if token exists in localStorage
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
   * Removes JWT token from localStorage
   * Called when user clicks logout button or session expires
   * After logout, user must login again to access protected routes
   */
  logout(): void {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.legacyTokenKey);
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
