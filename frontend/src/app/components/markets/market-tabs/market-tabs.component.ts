import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-market-tabs',
  templateUrl: './market-tabs.component.html',
  styleUrls: ['./market-tabs.component.scss'],
  imports: [CommonModule],
  standalone: true
})
export class MarketTabsComponent {
  @Input() markets: string[] = [];
  @Input() selectedMarket: string = '';
  @Output() marketChange = new EventEmitter<string>();

  onMarketSelect(market: string): void {
    this.marketChange.emit(market);
  }

  getCurrencyCode(market: string): string {
    const currencyMap: { [key: string]: string } = {
      'US': 'USD',
      'UK': 'GBP',
      'India': 'INR'
    };
    return currencyMap[market] || market;
  }
}
