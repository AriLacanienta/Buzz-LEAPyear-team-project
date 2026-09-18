import { Component } from '@angular/core';

@Component({
  selector: 'app-trade',
  templateUrl: './trade.component.html',
  styleUrls: ['./trade.component.scss']
})
export class TradeComponent {
  title = 'BuzzLEAPYear Trading Platform';

  constructor() {
    console.log('TradeComponent initialized');
  }
}
