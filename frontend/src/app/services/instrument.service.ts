import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '@environments/environment';

@Injectable({
  providedIn: 'root'
})
export class InstrumentService {
  private apiUrl = `${environment.apiUrl}/quote/market`;

  constructor(private http: HttpClient) {}

  getInstruments(assetType: string = 'EQUITY', page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}?assetType=${assetType}&page=${page}&size=${size}`);
  }
}