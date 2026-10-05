import { Component, Input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { AccountService } from '@app/services/account.service';
import { BalanceResponse } from '@app/models/balance-response.model';
import { switchMap, tap } from 'rxjs';

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.scss']
})
export class BalanceComponent {
    balance: BalanceResponse | null = null

    constructor(private accountService: AccountService) {}

    ngOnInit() {
      this.fetchBalance();
    }

    fetchBalance(): void {
      this.accountService.getMyAccount().pipe(
        tap(account => console.log('Account ID:', account.accountId)),
        switchMap(account =>
          this.accountService.getAccountBalance(account.accountId)
        )
      ).subscribe({
        next: balance => this.balance = balance,
        error: error => console.error('could not load account balance', error)
      });
    }
}
