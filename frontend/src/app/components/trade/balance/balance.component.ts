import { Component, DestroyRef, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { AccountService } from '@app/services/account.service';
import { BalanceResponse } from '@app/models/balance-response.model';
import { switchMap, tap, timer } from 'rxjs';
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

    constructor(private accountService: AccountService, private destroyRef: DestroyRef) {}

    ngOnInit() {
      this.fetchBalance();
    }

    fetchBalance(): void {
      timer(0, POLL_INTERVAL_MS).pipe(
        switchMap(() => this.accountService.getMyAccount()),
        switchMap(account => this.accountService.getAccountBalance(account.accountId)),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: balance => this.balance = balance,
        error: error => console.error('could not load account balance', error)
      });
    }
}
