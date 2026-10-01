import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class InstrumentService {
  private apiUrl = `${environment.apiUrl}/api/v1/quote/market`;

  constructor(private http: HttpClient) {}

  getInstruments(assetType: string = 'EQUITY', market: string | null = null, page: number = 0, size: number = 10): Observable<any> {
    let url = `${this.apiUrl}?assetType=${assetType}&page=${page}&size=${size}`;
    if (market) {
      url += `&market=${market}`;
    }
    return this.http.get<any>(url);
  }
}