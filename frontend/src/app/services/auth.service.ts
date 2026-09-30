import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

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

  constructor(private http: HttpClient) { }

  /**
   * Login user with email and password
   * 
   * Makes POST request to /api/auth/login
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
  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { email, password });
  }

  /**
   * Register new user
   * 
   * Makes POST request to /api/auth/register
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
           lastName: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, {
      username, email, firstName, lastName, password
    });
  }

  /**
   * Store JWT token in browser's localStorage
   * 
   * Called after successful login/register
   * Token is persisted even after browser closes
   * 
   * @param token - JWT token from backend
   */
  storeToken(token: string): void {
    localStorage.setItem('jwt_token', token);
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
    return localStorage.getItem('jwt_token');
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
    return this.getToken() !== null;
  }

  /**
   * Logout user
   * 
   * Removes JWT token from localStorage
   * Called when user clicks logout button or session expires
   * After logout, user must login again to access protected routes
   */
  logout(): void {
    localStorage.removeItem('jwt_token');
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
    const token = this.getToken();
    if (!token) {
      throw new Error('No token found');
    }
    return this.http.get(`${this.apiUrl}/validate`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
  }
}
