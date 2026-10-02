import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HoldingService {
  private apiUrl = 'http://localhost:6767/api/v1/accounts';

  constructor(private http: HttpClient) {}

  getAllHoldings(): Observable<any[]> {
    const url = `${this.apiUrl}/holdings`;
    console.log('Calling all holdings endpoint:', url);
    return this.http.get<any[]>(url);
  }

  getHoldingsByAccountId(accountId: string | number): Observable<any[]> {
    const url = `${this.apiUrl}/${accountId}/holdings`;
    console.log('Calling holdings endpoint:', url);
    return this.http.get<any[]>(url);
  }
}