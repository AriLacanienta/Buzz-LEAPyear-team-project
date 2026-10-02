import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

@Injectable({
  providedIn: 'root'
})
export class InstrumentService {
  private apiUrl = `${environment.apiUrl}/api/v1/quote`;

  constructor(private http: HttpClient) {}

  getInstruments(assetType: string = 'EQUITY', page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/market?assetType=${assetType}&page=${page}&size=${size}`);
  }
  
  searchInstruments(query: string): Observable<InstrumentSearchResponse[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<InstrumentSearchResponse[]>(`${this.apiUrl}/search`, { params });
  }
}