import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '@environments/environment';
import { AuthResponse } from '../models/auth-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private accessToken: string | null = null;

  constructor(private http: HttpClient) { }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }, {
      withCredentials: true
    }).pipe(tap(response => this.storeAuthResponse(response)));
  }

  register(username: string, email: string, firstName: string,
           lastName: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, {
      username, email, firstName, lastName, password
    }, {
      withCredentials: true
    }).pipe(tap(response => this.storeAuthResponse(response)));
  }

  refreshAccessToken(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, {
      withCredentials: true
    }).pipe(tap(response => this.storeAuthResponse(response)));
  }

  storeToken(token: string): void {
    this.accessToken = token;
  }

  storeAuthResponse(response: AuthResponse): void {
    this.storeToken(response.token);
    localStorage.setItem('auth_roles', JSON.stringify(response.roles ?? []));
  }

  getToken(): string | null {
    return this.accessToken;
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
      return Array.isArray(roles) ? roles : [];
    } catch {
      return [];
    }
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  isLoggedIn(): boolean {
    return this.accessToken !== null;
  }

  logout(): void {
    this.accessToken = null;
    localStorage.removeItem('auth_roles');
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({
      error: () => {
        // The local session is already cleared if the API call fails.
      }
    });
  }

  validateToken(): Observable<unknown> {
    const token = this.getAccessToken();
    if (!token) {
      throw new Error('No token found');
    }
    return this.http.get(`${this.apiUrl}/validate`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  }
}
