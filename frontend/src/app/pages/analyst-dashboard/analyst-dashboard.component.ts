import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, interval, merge, of } from 'rxjs';
import { catchError, map, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { AnalystTrade, AnalystTradePage } from '../../models/analyst-trade.model';
import { AnalystTradeService } from '../../services/analyst-trade.service';
import { HighchartsChartModule } from 'highcharts-angular';
import * as Highcharts from 'highcharts';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';

@Component({
  selector: 'app-analyst-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, HighchartsChartModule, DragDropModule],
  templateUrl: './analyst-dashboard.component.html',
  styleUrls: ['./analyst-dashboard.component.scss']
})
export class AnalystDashboardComponent implements OnInit, OnDestroy {
  readonly defaultWidgetOrder = ['summary', 'market-insights', 'charts', 'trades'];
  widgetOrder = [...this.defaultWidgetOrder];
  Highcharts: typeof Highcharts = Highcharts;
  assetMixChartOptions: Highcharts.Options = {};
  orderFlowChartOptions: Highcharts.Options = {};
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
  private readonly refresh$ = new Subject<void>();
  private requestVersion = 0;

  constructor(private analystTradeService: AnalystTradeService) {}

  ngOnInit(): void {
    this.loadWidgetOrder();
    merge(interval(2000), this.refresh$).pipe(
      startWith(0),
      map(() => ++this.requestVersion),
      switchMap(requestVersion => this.analystTradeService.getTrades(this.fromDate, this.toDate, this.searchTerm, this.pageIndex, this.pageSize).pipe(
        catchError((error: HttpErrorResponse) => {
          if (requestVersion !== this.requestVersion) {
            return of(null);
          }
          this.errorMessage = error.status === 403
            ? 'Analyst access is required to view this page.'
            : 'Unable to load trade activity. Check that the API is running.';
          this.isLoading = false;
          return of(null);
        })
      )),
      takeUntil(this.destroy$)
    ).subscribe(response => {
      if (response && response.content && response.number === this.pageIndex) {
        this.page = response;
        this.trades = response.content;
        this.updateInsightCharts();
        this.errorMessage = '';
        this.lastUpdated = new Date();
        this.isLoading = false;
      }
    });
  }

  dropWidget(event: CdkDragDrop<string[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(this.widgetOrder, event.previousIndex, event.currentIndex);
    localStorage.setItem('analyst-dashboard-widget-order', JSON.stringify(this.widgetOrder));
  }

  resetWidgetOrder(): void {
    this.widgetOrder = [...this.defaultWidgetOrder];
    localStorage.removeItem('analyst-dashboard-widget-order');
  }

  widgetPosition(widget: string): number {
    return this.widgetOrder.indexOf(widget);
  }

  trackByWidget(_: number, widget: string): string {
    return widget;
  }

  private loadWidgetOrder(): void {
    try {
      const saved = JSON.parse(localStorage.getItem('analyst-dashboard-widget-order') || 'null');
      if (Array.isArray(saved) && this.defaultWidgetOrder.every(widget => saved.includes(widget))) {
        this.widgetOrder = saved;
      }
    } catch {
      this.widgetOrder = [...this.defaultWidgetOrder];
    }
  }
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
    this.refreshTrades();
  }

  applyCustomRange(): void {
    this.selectedPreset = 'custom';
    this.pageIndex = 0;
    this.refreshTrades();
  }

  applySearch(): void {
    this.pageIndex = 0;
    this.refreshTrades();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.applySearch();
  }

  previousPage(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.refreshTrades();
    }
  }

  nextPage(): void {
    if (this.page && !this.page.last) {
      this.pageIndex++;
      this.refreshTrades();
    }
  }

  private refreshTrades(): void {
    this.trades = [];
    this.page = null;
    this.isLoading = true;
    this.errorMessage = '';
    this.requestVersion++;
    this.refresh$.next();
  }

  private updateInsightCharts(): void {
    const assetTotals = new Map<string, number>();
    this.trades.forEach(trade => {
      const assetType = trade.assetType || 'OTHER';
      assetTotals.set(assetType, (assetTotals.get(assetType) || 0) + 1);
    });

    const assetColors: Record<string, string> = {
      EQUITY: '#3B82F6',
      ETF: '#10B981',
      BOND: '#F59E0B',
      CRYPTO: '#EF4444',
      FUND: '#8B5CF6',
      OTHER: '#62748E'
    };

    const assetData = Array.from(assetTotals.entries()).map(([name, y]) => ({
      name,
      y,
      color: assetColors[name] || assetColors['OTHER']
    }));

    this.assetMixChartOptions = {
      chart: { type: 'pie', backgroundColor: 'transparent', height: 220 },
      title: { text: '' },
      credits: { enabled: false },
      tooltip: { pointFormat: '<b>{point.y}</b> trades ({point.percentage:.1f}%)' },
      plotOptions: {
        pie: {
          innerSize: '58%',
          borderWidth: 0,
          dataLabels: { enabled: true, distance: 8, style: { color: '#f1f5f9', textOutline: 'none', fontSize: '10px' } }
        }
      },
      legend: { enabled: false },
      series: [{ type: 'pie', name: 'Trades', data: assetData }]
    };

    const flowBuckets = new Map<string, { buy: number; sell: number; sortValue: number }>();
    this.trades.forEach(trade => {
      const date = new Date(trade.orderDate);
      const bucket = date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
      const current = flowBuckets.get(bucket) || { buy: 0, sell: 0, sortValue: date.getTime() };
      trade.side === 'BUY' ? current.buy++ : current.sell++;
      flowBuckets.set(bucket, current);
    });

    const flow = Array.from(flowBuckets.entries()).sort((first, second) => first[1].sortValue - second[1].sortValue);
    this.orderFlowChartOptions = {
      chart: { type: 'line', backgroundColor: 'transparent', height: 220 },
      title: { text: '' },
      credits: { enabled: false },
      xAxis: { categories: flow.map(([label]) => label), labels: { style: { color: '#62748E', fontSize: '10px' } }, lineColor: '#1E2D45', tickColor: '#1E2D45' },
      yAxis: { title: { text: '' }, allowDecimals: false, labels: { style: { color: '#62748E', fontSize: '10px' } }, gridLineColor: '#1E2D45' },
      tooltip: { shared: true, backgroundColor: 'rgba(15, 21, 32, 0.95)', borderColor: '#1E2D45', style: { color: '#FFFFFF' } },
      legend: { itemStyle: { color: '#9aa1ad' } },
      series: [
        { type: 'line', name: 'Buys', color: '#10B981', data: flow.map(([, values]) => values.buy) },
        { type: 'line', name: 'Sells', color: '#EF4444', data: flow.map(([, values]) => values.sell) }
      ]
    };
  }

  statusClass(status: string | null): string {
    return status ? status.toLowerCase() : 'unknown';
  }

  formatMoney(value: number, currency: string | null): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(value || 0);
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
