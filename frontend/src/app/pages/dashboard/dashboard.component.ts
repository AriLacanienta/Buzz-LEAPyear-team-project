import { Component, OnInit, OnDestroy } from '@angular/core';
import { HoldingService } from '@app/services/holding.service';
import { interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { PortfolioHoldingsContainerComponent } from '@app/components/portfolio/portfolio-holdings-container/portfolio-holdings-container.component';
import { SectionHeaderComponent } from '@app/components/dashboard/section-header/section-header.component';
import { MarketInfoCardComponent } from '@app/components/dashboard/market-info-card/market-info-card.component';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [CommonModule, PortfolioHoldingsContainerComponent, SectionHeaderComponent, MarketInfoCardComponent],
  standalone: true
})
export class DashboardComponent implements OnInit, OnDestroy {
  title = 'Buzz Trader';
  holdings: any[] = [];
  isLoading = true;
  accountId = 1;
  private refreshSubscription: Subscription | undefined;

  constructor(private holdingService: HoldingService) {
    console.log('DashboardComponent initialized');
  }

  ngOnInit(): void {
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
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading holdings:', error);
          console.error('Error details:', error.message, error.status);
          this.isLoading = false;
        }
      });
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }
}