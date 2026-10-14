import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { HoldingService } from '@app/services/holding.service';
import { InstrumentService } from '@app/services/instrument.service';
import { interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { PortfolioHoldingsContainerComponent } from '@app/components/client/portfolio/portfolio-holdings-container/portfolio-holdings-container.component';
import { MarketInfoCardComponent } from '@app/components/client/portfolio/market-info-card/market-info-card.component';
import { RecentOrdersComponent } from '@app/components/client/trade/recent-orders/recent-orders/recent-orders.component';
import { CurrentHoldingsComponent } from '@app/components/client/trade/current-holdings/current-holdings/current-holdings.component';
import { RecentOrderService } from '../../../services/recent-order.service';
import { AccountService } from '@app/services/account.service';
import { RecentOrder } from '../../../models/recent-order.model';

export interface QuoteResponse {
  symbol: string;
  price: number;
  changePercent: number;
  name: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [CommonModule, PortfolioHoldingsContainerComponent, MarketInfoCardComponent, RecentOrdersComponent, CurrentHoldingsComponent],
  standalone: true
})
export class DashboardComponent implements OnInit, OnDestroy {
  title = 'Buzz Trader';
  holdings = signal<any[]>([]);
  topGainers = signal<QuoteResponse[]>([]);
  topLosers = signal<QuoteResponse[]>([]);
  isLoading = signal(true);
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
    private accountService: AccountService
  ) {
    console.log('DashboardComponent initialized');
  }

  ngOnInit(): void {
    this.accountService.getMyAccount().subscribe({
      next: (account) => {
        this.accountId = Number(account.accountId);
        this.startDataRefresh();
        this.loadRecentOrders();
      },
      error: (error) => {
        console.error('Error loading current account:', error);
        this.isLoading.set(false);
      }
    });
  }

  private startDataRefresh(): void {
    const accountId = this.accountId;
    if (accountId === null) {
      this.isLoading.set(false);
      return;
    }

    this.refreshSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.holdingService.getHoldingsByAccountId(accountId))
      )
      .subscribe({
        next: (response: any) => {
          console.log('Holdings API Response:', response);
          console.log('Holdings count:', response?.length || 0);
          this.holdings.set((Array.isArray(response) ? response : [])
            .sort((a: any, b: any) => b.totalValue - a.totalValue)
            .slice(0, 5));
          this.isLoading.set(false);
        },
        error: (error) => {
          console.error('Error loading holdings:', error);
          console.error('Error details:', error.message, error.status);
          this.isLoading.set(false);
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
          this.topGainers.set(gainers);
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
          this.topLosers.set(losers);
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