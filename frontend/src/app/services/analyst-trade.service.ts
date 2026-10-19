import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AnalystTradePage } from '../models/analyst-trade.model';

@Injectable({ providedIn: 'root' })
export class AnalystTradeService {
  private readonly apiUrl = `${environment.apiUrl}/analyst/trades`;

  constructor(private http: HttpClient) {}

  getTrades(from: string, to: string, page = 0, size = 25): Observable<AnalystTradePage> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<AnalystTradePage>(this.apiUrl, { params });
  }
}
