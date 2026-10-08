import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

@Injectable({
  providedIn: 'root'
})
export class HoldingService {
  private apiUrl = `${environment.apiUrl}/holdings`;

  constructor(private http: HttpClient) {}

  getAllHoldings(): Observable<any[]> {
    const url = `${this.apiUrl}`;
    return this.http.get<any[]>(url);
  }

  getHoldingsByAccountId(accountId: string | number): Observable<any[]> {
    const url = `${this.apiUrl}/${encodeURIComponent(accountId)}`;
    return this.http.get<any[]>(url);
  }
}