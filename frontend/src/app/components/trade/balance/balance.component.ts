import { Component, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.scss']
})
export class BalanceComponent implements OnInit {
    cashBalance: number = 42100;
    portfolioValue: number = 289914;
    currencyType: string = 'USD';

    constructor() {}

    ngOnInit(): void {}


}
