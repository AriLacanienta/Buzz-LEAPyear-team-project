import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HighchartsChartModule } from 'highcharts-angular';
import * as Highcharts from 'highcharts';

import { HoldingService } from '../../../services/holding.service';
import { InstrumentService } from '../../../services/instrument.service';
import { RecentOrderService } from '../../../services/recent-order.service';
import { AccountService } from '../../../services/account.service';
import { WatchlistService } from '../../../services/watchlist.service';
import { WatchlistItem } from '../../../models/watchlist-item.model';
import { RecentOrder } from '../../../models/recent-order.model';
import { PortfolioHoldingsContainerComponent } from '@app/components/client/portfolio/portfolio-holdings-container/portfolio-holdings-container.component';
import { MarketInfoCardComponent } from '@app/components/client/portfolio/market-info-card/market-info-card.component';
import { RecentOrdersComponent } from '@app/components/client/trade/recent-orders/recent-orders/recent-orders.component';
import { CurrentHoldingsComponent } from '@app/components/client/trade/current-holdings/current-holdings/current-holdings.component';
import { forkJoin, interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { ChartHistoryService, ChartSample } from '@app/services/chart-history.service';

export interface QuoteResponse {
  symbol: string;
  price: number | null;
  changePercent: number | null;
  name: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    CommonModule,
    HighchartsChartModule,
    PortfolioHoldingsContainerComponent,
    MarketInfoCardComponent,
    RecentOrdersComponent,
    CurrentHoldingsComponent,
    DragDropModule
  ],
  standalone: true
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly defaultWidgetOrder = ['portfolio', 'holdings', 'markets', 'orders', 'current-holdings'];
  widgetOrder = [...this.defaultWidgetOrder];
  title = 'Buzz Trader';
  holdings: any[] = [];
  topGainers: QuoteResponse[] = [];
  topLosers: QuoteResponse[] = [];
  watchlist: QuoteResponse[] = [];
  isLoading = true;
  accountId: number | null = null;
  private refreshSubscription: Subscription | undefined;
  private gainersSubscription: Subscription | undefined;
  private losersSubscription: Subscription | undefined;
  private watchlistSubscription: Subscription | undefined;
  recentOrders: RecentOrder[] = [];
  private orderSubscription: Subscription | undefined;
  
  constructor(
    private holdingService: HoldingService,
    private instrumentService: InstrumentService,
    private recentOrderService: RecentOrderService,
    private accountService: AccountService,
    private watchlistService: WatchlistService,
    private chartHistory: ChartHistoryService,
    private cdr: ChangeDetectorRef
  ) {
  }

  Math = Math;

  // Portfolio balance chart tracking
  Highcharts: typeof Highcharts = Highcharts;
  portfolioChartOptions: Highcharts.Options = {};
  updateFlag = false;
  private portfolioBalanceHistory: ChartSample[] = [];
  totalPortfolioValue: number = 0;
  private currentPortfolioId: string = 'all-holdings';

  // Portfolio stats tracking
  totalCostBasis: number = 0;
  todayChange: number = 0;
  todayChangePercent: number = 0;
  allTimeChange: number = 0;
  allTimeChangePercent: number = 0;
  private previousPortfolioValue: number | null = null;

 

  ngOnInit(): void {
    this.loadWidgetOrder();
    this.portfolioChartOptions = {
      chart: { type: 'areaspline', backgroundColor: 'transparent', height: 300 },
      series: [{ type: 'areaspline', data: [] }],
      credits: { enabled: false }
    };

    this.accountService.getMyAccount().subscribe({
      next: (account) => {
        this.accountId = Number(account.accountId);
        this.portfolioBalanceHistory = this.chartHistory.read(this.accountId, 'portfolio-value');
        if (this.portfolioBalanceHistory.length) this.generatePortfolioChartOptions();
        this.startDataRefresh();
        this.loadRecentOrders();
      },
      error: (error) => {
        console.error('Error loading current account:', error);
        this.isLoading = false;
      }
    });
  }

  dropWidget(event: CdkDragDrop<string[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(this.widgetOrder, event.previousIndex, event.currentIndex);
    localStorage.setItem('client-dashboard-widget-order', JSON.stringify(this.widgetOrder));
  }

  resetWidgetOrder(): void {
    this.widgetOrder = [...this.defaultWidgetOrder];
    localStorage.removeItem('client-dashboard-widget-order');
  }

  widgetPosition(widget: string): number {
    return this.widgetOrder.indexOf(widget);
  }

  private loadWidgetOrder(): void {
    try {
      const saved = JSON.parse(localStorage.getItem('client-dashboard-widget-order') || 'null');
      if (Array.isArray(saved) && this.defaultWidgetOrder.every(widget => saved.includes(widget))) {
        this.widgetOrder = saved;
      }
    } catch {
      this.widgetOrder = [...this.defaultWidgetOrder];
    }
  }

  private startDataRefresh(): void {
    const accountId = this.accountId;
    if (accountId === null) {
      this.isLoading = false;
      return;
    }
    this.refreshSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => forkJoin({
          holdings: this.holdingService.getHoldingsByAccountId(accountId),
          summary: this.accountService.getPortfolioSummary(accountId)
        }))
      )
      .subscribe({
        next: ({ holdings, summary }) => {
          const observedAt = Date.now();
          (Array.isArray(holdings) ? holdings : []).forEach(holding => {
            if (holding.instrumentSymbol) {
              this.chartHistory.record(accountId, `holding-value:${holding.instrumentSymbol}`,
                Number(holding.totalValue), observedAt);
            }
          });
          this.holdings = (Array.isArray(holdings) ? holdings : [])
            .sort((a: any, b: any) => b.totalValue - a.totalValue)
            .slice(0, 10);
          
          this.totalPortfolioValue = Number(summary.portfolioValue);
          this.totalCostBasis = Number(summary.totalCostBasis);
          this.todayChange = Number(summary.todayChange);
          this.todayChangePercent = Number(summary.todayChangePercent);
          this.allTimeChange = Number(summary.allTimeChange);
          this.allTimeChangePercent = Number(summary.allTimeChangePercent);

          this.updatePortfolioChart();
          this.cdr.detectChanges();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading holdings:', error);
          console.error('Error details:', error.message, error.status);
          this.isLoading = false;
        }
      });

    this.gainersSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.instrumentService.getTopGainers(4))
      )
      .subscribe({
        next: (gainers) => {
          console.log('Top gainers:', gainers);
          this.topGainers = gainers.map(item => ({ ...item, name: item.name ?? item.symbol }));
        },
        error: (error) => {
          console.error('Error loading top gainers:', error);
        }
      });

    this.losersSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.instrumentService.getTopLosers(4))
      )
      .subscribe({
        next: (losers) => {
          console.log('Top losers:', losers);
          this.topLosers = losers.map(item => ({ ...item, name: item.name ?? item.symbol }));
        },
        error: (error) => {
          console.error('Error loading top losers:', error);
        }
      });

    this.watchlistSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.watchlistService.getWatchlist())
      )
      .subscribe({
        next: (items) => {
          this.watchlist = items.map((item: WatchlistItem) => ({
            symbol: item.symbol,
            price: item.price,
            changePercent: item.changePercent,
            name: item.instrumentName
          }));
        },
        error: (error) => {
          console.error('Error loading watchlist:', error);
        }
      });
  }

  private loadRecentOrders(): void {
    if (this.accountId !== null) {
      this.orderSubscription = this.recentOrderService.getRecentOrdersByAccountId(this.accountId, 20)
        .subscribe({
          next: (orders) => {
            console.log('Recent orders:', orders);
            this.recentOrders = orders;
          },
          error: (error) => {
            console.error('Error loading recent orders:', error);
          }
        });
    }
  }
trackBySymbol(index: number, item: any): string {
  return item ? item.instrumentSymbol : String(index); // Locks tracking lifecycle parameters to the stock symbol string identity
}

  private updatePortfolioChart(): void {
    if (this.accountId === null) return;
    this.portfolioBalanceHistory = this.chartHistory.record(
      this.accountId, 'portfolio-value', this.totalPortfolioValue);
    if (this.portfolioBalanceHistory.length) this.generatePortfolioChartOptions();
  }

    private averageByMinute(samples: ChartSample[]): ChartSample[] {
      const buckets = new Map<number, { total: number; count: number }>();

      for (const [timestamp, value] of samples) {
        const minute = Math.floor(timestamp / 5000) * 5000;
        const bucket = buckets.get(minute) ?? { total: 0, count: 0 };

        bucket.total += value;
        bucket.count++;
        buckets.set(minute, bucket);
      }

      return Array.from(buckets.entries())
        .sort(([a], [b]) => a - b)
        .map(([timestamp, bucket]): ChartSample => [
          timestamp,
          bucket.total / bucket.count
        ]);
    }

  private generatePortfolioChartOptions(): void {
    const chartSamples = this.averageByMinute(this.portfolioBalanceHistory);
    const values = chartSamples.map(([, value]) => value);
        // Dynamic range scaling - exact pattern from holding cards
    const minBalance = Math.min(...values);
    const maxBalance = Math.max(...values);
    const balanceRange = maxBalance - minBalance;
    const paddingBuffer = balanceRange > 0 ? balanceRange * 0.10 : 1.0;

    // Create fresh options object reference to force Angular change detection
    this.portfolioChartOptions = {
      chart: {
        type: 'areaspline',
        backgroundColor: 'transparent',
        height: 300,
        margin: [15, 10, 60, 60],
        spacing: [0, 0, 0, 0]
      },
      title: { text: '' },
      time: { timezone: Intl.DateTimeFormat().resolvedOptions().timeZone },
      xAxis: {
        type: 'datetime',
        title: { text: 'Time (local)', style: { color: '#62748E', fontSize: '11px' } },
        labels: {  format: '{value:%H:%M}', style: { color: '#62748E', fontSize: '10px' }},
        lineColor: '#1E2D45',
        tickColor: '#1E2D45',
        minPadding: 0,
        maxPadding: 0, 
        startOnTick: false,
        endOnTick: false
      },
      yAxis: {
        title: { text: '' },
        labels: { enabled: true, style: { color: '#62748E', fontSize: '11px' } },
        gridLineWidth: 1,
        gridLineColor: '#1E2D45',
        lineWidth: 0,
        tickWidth: 0,
        startOnTick: false,
        endOnTick: false,
        minPadding: 0.20,
        maxPadding: 0.20,
        min: minBalance - paddingBuffer,
        max: maxBalance + paddingBuffer
      },
      tooltip: {
        pointFormat: '<b>${point.y:.2f}</b>',
        xDateFormat: '%b %e, %Y %H:%M:%S',
        backgroundColor: 'rgba(15, 21, 32, 0.95)',
        borderColor: '#1E2D45',
        style: { color: '#FFFFFF' }
      },
      legend: { enabled: false },
      plotOptions: {
        areaspline: {
          dataLabels: { enabled: false },
          enableMouseTracking: true,
          animation: false
        }
      },
      series: [{
        type: 'areaspline' as any,
        name: 'Portfolio Balance',
        data: chartSamples,
        color: '#6366F1',
        lineWidth: 3,
        marker: {
          enabled: true,
          radius: 4,
          fillColor: '#6366F1'
        },
        fillColor: {
          linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
          stops: [
            [0, '#6366F1' + '40'],
            [1, '#6366F1' + '00']
          ]
        }
      }],
      credits: { enabled: false }
    };

    this.updateFlag = true;

    // Force change detection after chart options are created
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
    if (this.gainersSubscription) {
      this.gainersSubscription.unsubscribe();
    }
    if (this.losersSubscription) {
      this.losersSubscription.unsubscribe();
    }
    if (this.watchlistSubscription) {
      this.watchlistSubscription.unsubscribe();
    }
    if (this.orderSubscription) {
      this.orderSubscription.unsubscribe();
    }
  }
}