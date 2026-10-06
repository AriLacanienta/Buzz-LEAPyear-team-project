import { Component, OnInit, DestroyRef } from '@angular/core';
import { NgIf, NgFor, DatePipe, CurrencyPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap, timer } from 'rxjs';
import { AccountService } from '@app/services/account.service';
import { RecentTradeOrderResponse } from '@app/models/recent-trade-order-response.model';

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-recent-orders',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, CurrencyPipe],
  templateUrl: './recent-orders.component.html',
  styleUrl: './recent-orders.component.scss'
})
export class RecentOrdersComponent implements OnInit {
  orders: RecentTradeOrderResponse[] = [];

  constructor(
    private accountService: AccountService,
    private destroyRef: DestroyRef
  ) {}

  ngOnInit() {
    this.fetchRecentOrders();
  }

  private fetchRecentOrders(): void {
    timer(0, POLL_INTERVAL_MS).pipe(
      switchMap(() => this.accountService.getMyAccount()),
      switchMap(account => this.accountService.getRecentTradeOrders(account.accountId, 10)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: orders => this.orders = orders,
      error: error => console.error('could not load recent orders', error)
    });
  }
}
