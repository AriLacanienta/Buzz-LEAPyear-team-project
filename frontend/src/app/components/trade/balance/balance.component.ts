import { Component, Input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { BalanceResponse } from '@app/models/balance-response.model';

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.scss']
})
export class BalanceComponent {
    balance: BalanceResponse | null = null

    ngOnInit() {
      // Fetch from backend-----
      this.balance = {
        cashAvailable: 0,
        portfolioValue: 0,
        currencyType: 'USD'
      };
    }
}
