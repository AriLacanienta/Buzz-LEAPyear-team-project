import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { Component, OnInit, DestroyRef } from '@angular/core';
import { filter } from 'rxjs/operators';
import { TradeOrderService } from '@app/services/trade-order.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrentHoldingsResponse } from '@app/models/current-holdings-response.model';
import { AccountService } from '@app/services/account.service';
import { HoldingService } from '@app/services/holding.service';
import { merge, switchMap, timer } from 'rxjs';

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-current-holdings',
  standalone: true,
  imports: [NgFor, NgIf, CurrencyPipe],
  templateUrl: './current-holdings.component.html',
  styleUrl: './current-holdings.component.scss'
})
export class CurrentHoldingsComponent implements OnInit {
  currentHoldings: CurrentHoldingsResponse[] = [];

  constructor(
    private accountService: AccountService,
    private holdingService: HoldingService,
    private tradeOrderService: TradeOrderService,
    private destroyRef: DestroyRef
  ) {}

  ngOnInit() {
    this.fetchCurrentHoldings();
  }

  private fetchCurrentHoldings() {
    merge(
      timer(0, POLL_INTERVAL_MS),
      this.tradeOrderService.orderCompleted$.pipe(filter(status => status === 'FILLED'))
    ).pipe(
        switchMap(() => this.accountService.getMyAccount()),
        switchMap(account => this.holdingService.getHoldingsByAccountId(account.accountId)),
        takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: holdings => this.currentHoldings = holdings,
      error: error => console.error('could not load current holdings', error)
    });
  }
}
