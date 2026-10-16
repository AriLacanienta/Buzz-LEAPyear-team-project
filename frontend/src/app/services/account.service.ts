import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { BalanceResponse } from '@app/models/balance-response.model';
import { CurrentAccountResponse } from '@app/models/current-account-response.model';
import { RecentTradeOrderResponse, SpringPage } from '@app/models/recent-trade-order-response.model';
import { FundsRequest } from '@app/models/funds-request.model';
import { PortfolioSummaryResponse } from '@app/models/portfolio-summary-response.model';

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

  getPortfolioSummary(id: string | number): Observable<PortfolioSummaryResponse> {
    return this.http.get<PortfolioSummaryResponse>(`${this.apiUrl}/${encodeURIComponent(id)}/portfolio-summary`);
  }

  updateFunds(id: string, request: FundsRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${encodeURIComponent(id)}/funds`, request);
  }

  getRecentTradeOrders(accountId: string | number, page =0, size = 10): Observable<SpringPage<RecentTradeOrderResponse>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    const url = `${this.apiUrl}/${encodeURIComponent(accountId)}/tradeorders`;
    return this.http.get<SpringPage<RecentTradeOrderResponse>>(url, { params });
  }
}
