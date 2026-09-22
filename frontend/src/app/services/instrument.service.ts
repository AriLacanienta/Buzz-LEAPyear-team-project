// service for handling instrument-related operations
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

@Injectable({
  providedIn: 'root'
})
export class InstrumentService {
  private apiUrl = 'http://localhost:6767/api/v1/instruments';

  constructor(private http: HttpClient) { }

  getInstruments(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}?page=${page}&size=${size}`);
  }
}