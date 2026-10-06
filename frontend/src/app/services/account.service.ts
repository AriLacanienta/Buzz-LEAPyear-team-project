import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { BalanceResponse } from '@app/models/balance-response.model';
import { CurrentAccountResponse } from '@app/models/current-account-response.model';
import { RecentTradeOrderResponse } from '@app/models/recent-trade-order-response.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = `${environment.apiUrl}/accounts`;
  private accountNameSubject = new BehaviorSubject<string>('');
  accountName$ = this.accountNameSubject.asObservable();

  constructor(private http: HttpClient) {}

  getMyAccount(): Observable<CurrentAccountResponse> {
      return this.http.get<CurrentAccountResponse>(`${this.apiUrl}/me`);
  }

  setAccountName(name: string) {
    this.accountNameSubject.next(name);
  }

  getAccountName() {
    return this.accountNameSubject.value;
  }

  getAccountBalance(id: string): Observable<BalanceResponse> {
      return this.http.get<BalanceResponse>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }

  getRecentTradeOrders(accountId: string | number, limit = 10): Observable<RecentTradeOrderResponse[]> {
    const params = new HttpParams().set('limit', String(limit));
    const url = `${this.apiUrl}/${encodeURIComponent(accountId)}/tradeorders`;
    return this.http.get<RecentTradeOrderResponse[]>(url, { params });
  }
}
