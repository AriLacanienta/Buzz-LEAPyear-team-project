import { Component } from '@angular/core';
import { BalanceComponent } from '@app/components/client//trade/balance/balance.component';
import { RecentOrdersComponent } from '@app/components/client//trade/recent-orders/recent-orders/recent-orders.component';
import { BuySellComponent } from '@app/components/client//trade/buy-sell/buy-sell/buy-sell.component';
import { CurrentHoldingsComponent } from '@app/components/client//trade/current-holdings/current-holdings/current-holdings.component';

@Component({
  selector: 'app-trade',
  standalone: true,
  imports: [BalanceComponent, RecentOrdersComponent, BuySellComponent, CurrentHoldingsComponent],
  templateUrl: './trade.component.html',
  styleUrls: ['./trade.component.scss']
})
export class TradeComponent {
  
  constructor() {
    console.log('TradeComponent initialized');
  }
}
