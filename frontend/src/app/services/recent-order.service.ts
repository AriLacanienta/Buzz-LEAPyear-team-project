import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RecentOrder } from '../models/recent-order.model';

@Injectable({
  providedIn: 'root'
})
export class RecentOrderService {
  private apiUrl = '/api/v1/tradeorders/accounts';

  constructor(private http: HttpClient) { }

  getRecentOrdersByAccountId(accountId: number, limit: number = 20): Observable<RecentOrder[]> {
    return this.http.get<RecentOrder[]>(`${this.apiUrl}/${accountId}/orders?limit=${limit}`);
  }
}