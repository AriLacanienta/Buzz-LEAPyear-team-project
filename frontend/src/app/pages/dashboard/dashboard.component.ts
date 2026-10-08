import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { HoldingService } from '@app/services/holding.service';
import { interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import * as Highcharts from 'highcharts';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  title = 'Buzz Trader';
  holdings: any[] = [];
  isLoading = true;
  accountId = 1;
  private refreshSubscription: Subscription | undefined;
  Math = Math;

  // Portfolio balance chart tracking
  Highcharts: typeof Highcharts = Highcharts;
  portfolioChartOptions: Highcharts.Options = {};
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

  constructor(private holdingService: HoldingService, private cdr: ChangeDetectorRef) {
    console.log('DashboardComponent initialized');
  }

  ngOnInit(): void {
    // Initialize empty chart options immediately
    this.portfolioChartOptions = {
      chart: { type: 'areaspline', backgroundColor: 'transparent', height: 300 },
      series: [{ type: 'areaspline' as any, data: [] }],
      credits: { enabled: false }
    };

    this.refreshSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.holdingService.getHoldingsByAccountId(this.accountId))
      )
      .subscribe({
        next: (response: any) => {
          console.log('Holdings API Response:', response);
          console.log('Holdings count:', response?.length || 0);
          this.holdings = (Array.isArray(response) ? response : [])
            .sort((a: any, b: any) => b.totalValue - a.totalValue)
            .slice(0, 5);
          
          // Calculate total portfolio value from ALL holdings, not just top 5
          const allHoldings = Array.isArray(response) ? response : [];
          
          // Calculate new portfolio value
          const newPortfolioValue = Number(
            allHoldings
              .reduce((sum: number, holding: any) => sum + (holding.totalValue || 0), 0)
              .toFixed(2)
          );
          
          // Calculate total cost basis (initial investment)
          this.totalCostBasis = Number(
            allHoldings
              .reduce((sum: number, holding: any) => sum + (holding.totalCost || 0), 0)
              .toFixed(2)
          );
          
          // Calculate all-time change
          this.allTimeChange = Number((newPortfolioValue - this.totalCostBasis).toFixed(2));
          this.allTimeChangePercent = this.totalCostBasis > 0 
            ? Number(((this.allTimeChange / this.totalCostBasis) * 100).toFixed(2))
            : 0;
          
          // Calculate today's change
          if (this.previousPortfolioValue === null) {
            this.previousPortfolioValue = newPortfolioValue;
            this.todayChange = 0;
            this.todayChangePercent = 0;
          } else {
            this.todayChange = Number((newPortfolioValue - this.previousPortfolioValue).toFixed(2));
            this.todayChangePercent = this.previousPortfolioValue > 0
              ? Number(((this.todayChange / this.previousPortfolioValue) * 100).toFixed(2))
              : 0;
          }
          
          this.totalPortfolioValue = newPortfolioValue;
          
          console.log('Total Portfolio Value:', this.totalPortfolioValue);
          console.log('Today Change:', this.todayChange, this.todayChangePercent + '%');
          console.log('All Time Change:', this.allTimeChange, this.allTimeChangePercent + '%');
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
      console.log('Portfolio chart initialized:', this.portfolioBalanceHistory);
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
        
        console.log('Portfolio value appended:', this.totalPortfolioValue);
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

    console.log('Portfolio chart options generated:', {
      dataPoints: this.portfolioBalanceHistory.length,
      data: this.portfolioBalanceHistory,
      minBalance,
      maxBalance
    });
    
    // Force change detection after chart options are created
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }
}