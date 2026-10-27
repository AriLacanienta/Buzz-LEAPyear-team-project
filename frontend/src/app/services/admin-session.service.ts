import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ActiveSession {
    userId: number;
    username: string;
}

@Injectable({ providedIn: 'root' })
export class AdminSessionService {
    private apiUrl = `${environment.apiUrl}/admin/sessions`;

    constructor(private http: HttpClient) {}

    getActiveSessions(userId?: number): Observable<ActiveSession[]> {
        let params = new HttpParams();

        if (userId !== undefined) {
            params = params.set('userId', String(userId));
        }

        return this.http.get<ActiveSession[]>(this.apiUrl, { params });
    }

    // revokes the user's refresh tokens and invalidates their current access tokens
    revokeUserTokens(userId: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${userId}`);
    }
}
