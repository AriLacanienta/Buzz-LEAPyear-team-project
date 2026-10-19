import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, interval, of } from 'rxjs';
import { catchError, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { AnalystTrade, AnalystTradePage } from '../../models/analyst-trade.model';
import { AnalystTradeService } from '../../services/analyst-trade.service';

@Component({
  selector: 'app-analyst-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './analyst-dashboard.component.html',
  styleUrls: ['./analyst-dashboard.component.scss']
})
export class AnalystDashboardComponent implements OnInit, OnDestroy {
  trades: AnalystTrade[] = [];
  page: AnalystTradePage | null = null;
  fromDate = this.today();
  toDate = this.today();
  searchTerm = '';
  selectedPreset = 'today';
  pageIndex = 0;
  readonly pageSize = 100;
  isLoading = true;
  lastUpdated: Date | null = null;
  errorMessage = '';
  private readonly destroy$ = new Subject<void>();

  constructor(private analystTradeService: AnalystTradeService) {}

  get buyCount(): number {
    return this.trades.filter(trade => trade.side === 'BUY').length;
  }

  get sellCount(): number {
    return this.trades.filter(trade => trade.side === 'SELL').length;
  }

  get totalQuantity(): number {
    return this.trades.reduce((total, trade) => total + Number(trade.quantity || 0), 0);
  }

  get mostTradedStocks(): Array<{ symbol: string; trades: number; quantity: number }> {
    const totals = new Map<string, { trades: number; quantity: number }>();
    this.trades.forEach(trade => {
      const current = totals.get(trade.instrumentSymbol) || { trades: 0, quantity: 0 };
      current.trades++;
      current.quantity += Number(trade.quantity || 0);
      totals.set(trade.instrumentSymbol, current);
    });

    return Array.from(totals.entries())
      .map(([symbol, totalsForSymbol]) => ({ symbol, ...totalsForSymbol }))
      .sort((first, second) => second.trades - first.trades || second.quantity - first.quantity)
      .slice(0, 5);
  }

  get maxStockTrades(): number {
    return this.mostTradedStocks[0]?.trades || 1;
  }

  ngOnInit(): void {
    interval(2000).pipe(
      startWith(0),
      switchMap(() => this.analystTradeService.getTrades(this.fromDate, this.toDate, this.searchTerm, this.pageIndex, this.pageSize).pipe(
        catchError((error: HttpErrorResponse) => {
          this.errorMessage = error.status === 403
            ? 'Analyst access is required to view this page.'
            : 'Unable to load trade activity. Check that the API is running.';
          this.isLoading = false;
          return of(null);
        })
      )),
      takeUntil(this.destroy$)
    ).subscribe(response => {
      if (response) {
        this.page = response;
        this.trades = response.content;
        this.errorMessage = '';
        this.lastUpdated = new Date();
        this.isLoading = false;
      }
    });
  }

  choosePreset(preset: string): void {
    this.selectedPreset = preset;
    const today = new Date();
    this.toDate = this.formatDate(today);
    if (preset === 'today') {
      this.fromDate = this.toDate;
    } else {
      today.setDate(today.getDate() - 6);
      this.fromDate = this.formatDate(today);
    }
    this.pageIndex = 0;
  }

  applyCustomRange(): void {
    this.selectedPreset = 'custom';
    this.pageIndex = 0;
  }

  applySearch(): void {
    this.pageIndex = 0;
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.applySearch();
  }

  previousPage(): void {
    if (this.pageIndex > 0) this.pageIndex--;
  }

  nextPage(): void {
    if (this.page && !this.page.last) this.pageIndex++;
  }

  statusClass(status: string | null): string {
    return status ? status.toLowerCase() : 'unknown';
  }

  formatMoney(value: number, currency: string): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value || 0);
  }

  trackByOrderId(_: number, trade: AnalystTrade): number {
    return trade.orderId;
  }

  private today(): string {
    return this.formatDate(new Date());
  }

  private formatDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
