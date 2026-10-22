import { Component, DestroyRef, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { AccountService } from '@app/services/account.service';
import { TradeOrderService } from '@app/services/trade-order.service';
import { BalanceResponse } from '@app/models/balance-response.model';
import { map, switchMap, tap, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.scss']
})
export class BalanceComponent implements OnInit {
    balance: BalanceResponse | null = null

    constructor(
      private accountService: AccountService,
      private tradeOrderService: TradeOrderService,
      private destroyRef: DestroyRef
    ) {}

    ngOnInit() {
      this.refreshPortfolioValue();
      this.refreshCashBalance();
    }

    private refreshPortfolioValue(): void {
      timer(0, POLL_INTERVAL_MS).pipe(
        switchMap(() => this.loadBalance()),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: latest => this.balance = {
          cashAvailable: this.balance?.cashAvailable ?? latest.cashAvailable,
          cashReserved: this.balance?.cashReserved ?? latest.cashReserved,
          portfolioValue: latest.portfolioValue,
        },
        error: error => console.error('could not load portfolio value', error)
      });
    }

    refreshCashBalance(): void {
      this.tradeOrderService.orderCompleted$.pipe(
        switchMap(status => this.loadBalance().pipe(map(latest => ({ latest, status })))),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: ({ latest, status }) => this.balance = {
          cashAvailable: latest.cashAvailable,
          cashReserved: latest.cashReserved,
          portfolioValue: status === 'FILLED' || this.balance?.portfolioValue == null 
          ? latest.portfolioValue 
          : this.balance?.portfolioValue,
        },
        error: error => console.error('could not refresh cash balance', error)
      })
    }

    private loadBalance() {
      return this.accountService.getMyAccount().pipe(
        switchMap(account => this.accountService.getAccountBalance(account.accountId))
      )
    }
}
