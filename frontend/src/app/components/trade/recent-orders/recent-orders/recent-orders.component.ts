import { Component, OnInit, DestroyRef } from '@angular/core';
import { NgIf, NgFor, DatePipe, CurrencyPipe } from '@angular/common';
import { merge, of, switchMap, timer, exhaustMap, retry, takeWhile, Subject } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AccountService } from '@app/services/account.service';
import { RecentTradeOrderResponse } from '@app/models/recent-trade-order-response.model';
import { TradeOrderService } from '@app/services/trade-order.service';

@Component({
  selector: 'app-recent-orders',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, CurrencyPipe],
  templateUrl: './recent-orders.component.html',
  styleUrl: './recent-orders.component.scss'
})
export class RecentOrdersComponent implements OnInit {
  orders: RecentTradeOrderResponse[] = [];
  pageIndex = 0;
  pageSize = 9;
  totalElements = 0;
  totalPages = 0;
  private pageChanged$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private tradeOrderService: TradeOrderService,
    private destroyRef: DestroyRef
  ) {}

  ngOnInit() {
    this.fetchRecentOrders();
  }

  private fetchRecentOrders(): void {
    this.tradeOrderService.orderPlaced$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.pageIndex = 0;
      this.pageChanged$.next();
    });

    merge(of(null), this.pageChanged$).pipe(
      switchMap(() => timer(0, 2000).pipe(
        exhaustMap(() => this.accountService.getMyAccount().pipe(
          switchMap(account => this.accountService.getRecentTradeOrders(account.accountId, this.pageIndex, this.pageSize)),
          retry({ delay: 2000 })
        )),
        takeWhile(page => page.content.some(order => this.isPending(order.status)), true)
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: page => {
        this.orders = page.content;
        this.pageIndex = page.number;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
      },
      error: error => console.error('could not load recent orders', error)
    });
  }

  previousPage(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.pageChanged$.next();
    }
  }

  nextPage(): void {
    if (this.pageIndex + 1 < this.totalPages) {
      this.pageIndex++;
      this.pageChanged$.next();
    }
  }

  get firstOrderNumber(): number {
    return this.totalElements === 0 ? 0 : this.pageIndex * this.pageSize + 1;
  }

  get lastOrderNumber(): number {
    return Math.min((this.pageIndex + 1) * this.pageSize, this.totalElements);
  }

  trackByOrderId(_: number, order: RecentTradeOrderResponse): number {
    return order.orderId;
  }

  private isPending(status: RecentTradeOrderResponse['status']): boolean {
    return status === 'SUBMITTED' || status === 'ACCEPTED' || status === 'VALIDATED';
  }
}