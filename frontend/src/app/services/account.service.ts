import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BalanceResponse } from '@app/models/balance-response.model';
import { CurrentAccountResponse } from '@app/models/current-account-response.model';
import { RecentTradeOrderResponse, SpringPage } from '@app/models/recent-trade-order-response.model';
import { FundsRequest } from '@app/models/funds-request.model';
import { PortfolioSummaryResponse } from '@app/models/portfolio-summary-response.model';

export interface UserProfile {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly apiUrl = `${environment.apiUrl}/accounts`;
  private readonly userApiUrl = `${environment.apiUrl}/user`;
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
  getUserProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.userApiUrl}/profile/${username}`);
  }
}
