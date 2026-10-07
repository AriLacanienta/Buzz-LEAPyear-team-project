import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { RecentOrder } from '@app/models/recent-order.model';
import { RecentOrderService } from '@app/services/recent-order.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-recent-orders',
  templateUrl: './recent-orders.component.html',
  styleUrls: ['./recent-orders.component.scss'],
  standalone: true,
  imports: [CommonModule, MatTableModule]
})
export class RecentOrdersComponent implements OnInit, OnDestroy {
  @Input() data: RecentOrder[] = [];
  dataSource = new MatTableDataSource<RecentOrder>();

  displayedColumns: string[] = ['tradeId', 'tradeDate', 'side', 'instrument', 'assetClass', 'quantity', 'currency', 'price'];
  isLoading = false;
  errorMessage: string | null = null;
  private subscription: Subscription | undefined;
  private accountId = 1; // Default account ID
  
  constructor(private recentOrderService: RecentOrderService) {}

  ngOnInit(): void {
    this.loadRecentOrders();
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  private loadRecentOrders(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.subscription = this.recentOrderService.getRecentOrdersByAccountId(this.accountId, 20).subscribe({
      next: (orders: RecentOrder[]) => {
        console.log('Recent orders loaded:', orders);
        this.data = orders;
        this.dataSource.data = orders;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading recent orders:', error);
        this.errorMessage = 'Failed to load recent orders';
        this.isLoading = false;
      }
    });
  }

  formatColumnHeader(column: string): string {
    return column
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }

  getSideColor(side: string): string {
    switch (side.toLowerCase()) {
      case 'buy':
        return 'buy';
      case 'sell':
        return 'sell';
      default:
        return 'inherit';
    }
  }
}