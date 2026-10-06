import { CurrencyPipe, NgFor } from '@angular/common';
import { Component, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrentHoldingsResponse } from '@app/models/current-holdings-response.model';
import { AccountService } from '@app/services/account.service';
import { HoldingService } from '@app/services/holding.service';
import { switchMap, timer } from 'rxjs';

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-current-holdings',
  standalone: true,
  imports: [NgFor, CurrencyPipe],
  templateUrl: './current-holdings.component.html',
  styleUrl: './current-holdings.component.scss'
})
export class CurrentHoldingsComponent implements OnInit {
  currentHoldings: CurrentHoldingsResponse[] = [];

  constructor(
    private accountService: AccountService,
    private holdingService: HoldingService,
    private destroyRef: DestroyRef
  ) {}

  ngOnInit() {
    this.fetchCurrentHoldings();
  }

  private fetchCurrentHoldings() {
    timer(0, POLL_INTERVAL_MS).pipe(
      switchMap(() => this.accountService.getMyAccount()),
      switchMap(account => this.holdingService.getHoldingsByAccountId(account.accountId)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: holdings => this.currentHoldings = holdings,
      error: error => console.error('could not load current holdings', error)
    });
  }
}
