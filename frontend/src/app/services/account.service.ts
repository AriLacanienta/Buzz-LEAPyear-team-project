import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { BalanceResponse } from '@app/models/balance-response.model';
import { CurrentAccountResponse } from '@app/models/current-account-response.model';
import { RecentTradeOrderResponse, SpringPage } from '@app/models/recent-trade-order-response.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = `${environment.apiUrl}/accounts`;
  private accountNameState = signal('');
  accountName = this.accountNameState.asReadonly();

  constructor(private http: HttpClient) {}

  getMyAccount(): Observable<CurrentAccountResponse> {
      return this.http.get<CurrentAccountResponse>(`${this.apiUrl}/me`);
  }

  setAccountName(name: string): void {
    this.accountNameState.set(name);
  }

  getAccountName(): string {
    return this.accountNameState();
  }

  getAccountBalance(id: string): Observable<BalanceResponse> {
      return this.http.get<BalanceResponse>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }

  getRecentTradeOrders(accountId: string | number, page =0, size = 10): Observable<SpringPage<RecentTradeOrderResponse>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    const url = `${this.apiUrl}/${encodeURIComponent(accountId)}/tradeorders`;
    return this.http.get<SpringPage<RecentTradeOrderResponse>>(url, { params });
  }
}
