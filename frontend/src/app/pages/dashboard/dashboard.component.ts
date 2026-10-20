import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { HoldingService } from '@app/services/holding.service';
import { InstrumentService } from '@app/services/instrument.service';
import { forkJoin, interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { PortfolioHoldingsContainerComponent } from '@app/components/portfolio/portfolio-holdings-container/portfolio-holdings-container.component';
import { PortfolioHoldingCardComponent } from '@app/components/portfolio/portfolio-holding-card/portfolio-holding-card.component';
import { MarketInfoCardComponent } from '@app/components/portfolio/market-info-card/market-info-card.component';
import { RecentOrdersComponent } from '@app/components/trade/recent-orders/recent-orders/recent-orders.component';
import { CurrentHoldingsComponent } from '@app/components/trade/current-holdings/current-holdings/current-holdings.component';
import { RecentOrderService } from '../../services/recent-order.service';
import { AccountService } from '@app/services/account.service';
import { RecentOrder } from '../../models/recent-order.model';
import { HighchartsChartModule } from 'highcharts-angular';

export interface QuoteResponse {
  symbol: string;
  price: number;
  changePercent: number;
  name: string;
}
import * as Highcharts from 'highcharts';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    CommonModule,
    HighchartsChartModule,
    PortfolioHoldingsContainerComponent,
    PortfolioHoldingCardComponent,
    MarketInfoCardComponent,
    RecentOrdersComponent,
    CurrentHoldingsComponent
  ],
  standalone: true
})
export class DashboardComponent implements OnInit, OnDestroy {
  title = 'Buzz Trader';
  holdings: any[] = [];
  topGainers: QuoteResponse[] = [];
  topLosers: QuoteResponse[] = [];
  isLoading = true;
  accountId: number | null = null;
  private refreshSubscription: Subscription | undefined;
  private gainersSubscription: Subscription | undefined;
  private losersSubscription: Subscription | undefined;
  recentOrders: RecentOrder[] = [];
  private orderSubscription: Subscription | undefined;
  
  constructor(
    private holdingService: HoldingService,
    private instrumentService: InstrumentService,
    private recentOrderService: RecentOrderService,
    private accountService: AccountService,
    private cdr: ChangeDetectorRef
  ) {
  }

  Math = Math;

  // Portfolio balance chart tracking
  Highcharts: typeof Highcharts = Highcharts;
  portfolioChartOptions: Highcharts.Options = {};
  updateFlag = false;
  private portfolioBalanceHistory: number[] = [];
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
    this.portfolioChartOptions = {
      chart: { type: 'areaspline', backgroundColor: 'transparent', height: 300 },
      series: [{ type: 'areaspline', data: [] }],
      credits: { enabled: false }
    };

    this.accountService.getMyAccount().subscribe({
      next: (account) => {
        this.accountId = Number(account.accountId);
        this.startDataRefresh();
        this.loadRecentOrders();
      },
      error: (error) => {
        console.error('Error loading current account:', error);
        this.isLoading = false;
      }
    });
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
          this.topGainers = gainers;
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
          this.topLosers = losers;
        },
        error: (error) => {
          console.error('Error loading top losers:', error);
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
    const portfolioId = 'all-holdings';

    if (this.portfolioBalanceHistory.length === 0 || this.currentPortfolioId !== portfolioId) {
      this.currentPortfolioId = portfolioId;

      // Initialize with 6 data points like holding cards do
      const randomFactor = 0.004;
      const baseVariations = [-4, -2, -3.5, 1, -1.5, 0];

      this.portfolioBalanceHistory = baseVariations.map(variant => {
        const simulatedValue = this.totalPortfolioValue * (1 + (variant * randomFactor));
        return Number(simulatedValue.toFixed(2));
      });

      // Force final point to be the real current value
      this.portfolioBalanceHistory[this.portfolioBalanceHistory.length - 1] = this.totalPortfolioValue;
      this.generatePortfolioChartOptions();
    } else {
      // Real time updates - append new value if different
      const lastSavedValue = this.portfolioBalanceHistory[this.portfolioBalanceHistory.length - 1];
      if (this.totalPortfolioValue !== lastSavedValue) {
        this.portfolioBalanceHistory.push(this.totalPortfolioValue);

        // Keep last 8 data points like holding cards
        if (this.portfolioBalanceHistory.length > 8) {
          this.portfolioBalanceHistory.shift();
        }

        this.generatePortfolioChartOptions();
      }
    }
  }

  private generatePortfolioChartOptions(): void {
    const times = this.portfolioBalanceHistory.map((_, i) => {
      const now = new Date();
      const time = new Date(now.getTime() - (this.portfolioBalanceHistory.length - i - 1) * 5 * 60000);
      return time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    });

    // Dynamic range scaling - exact pattern from holding cards
    const minBalance = Math.min(...this.portfolioBalanceHistory);
    const maxBalance = Math.max(...this.portfolioBalanceHistory);
    const balanceRange = maxBalance - minBalance;
    const paddingBuffer = balanceRange > 0 ? balanceRange * 0.10 : 1.0;

    // Create fresh options object reference to force Angular change detection
    this.portfolioChartOptions = {
      chart: {
        type: 'areaspline',
        backgroundColor: 'transparent',
        height: 300,
        margin: [15, 0, 15, 50],
        spacing: [0, 0, 0, 0]
      },
      title: { text: '' },
      xAxis: {
        categories: times,
        visible: false,
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
        headerFormat: '<b>Portfolio Value</b><br>',
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
        data: [...this.portfolioBalanceHistory],
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
    if (this.orderSubscription) {
      this.orderSubscription.unsubscribe();
    }
  }
}
