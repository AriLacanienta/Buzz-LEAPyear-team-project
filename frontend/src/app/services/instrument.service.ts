import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

export interface QuoteResponse {
  symbol: string;
  price: number;
  changePercent: number;
}

@Injectable({
  providedIn: 'root'
})
export class InstrumentService {
  private apiUrl = `${environment.apiUrl}/quote`;

  constructor(private http: HttpClient) {}
  
  searchInstruments(query: string): Observable<InstrumentSearchResponse[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<{ content: InstrumentSearchResponse[] }>(`${this.apiUrl}/search`, { params }).pipe(
      map(page => page.content ?? [])
    );
  }

  getQuote(symbol: string): Observable<QuoteResponse> {
    return this.http.get<QuoteResponse>(`${this.apiUrl}/${encodeURIComponent(symbol)}`);
  }

  getInstruments(assetType: string = 'EQUITY', market: string | null = null, page: number = 0, size: number = 10): Observable<any> {
    let url = `${this.apiUrl}/market?assetType=${assetType}&page=${page}&size=${size}`;
    if (market) {
      url += `&market=${market}`;
    }
    return this.http.get<any>(url);
  }
}