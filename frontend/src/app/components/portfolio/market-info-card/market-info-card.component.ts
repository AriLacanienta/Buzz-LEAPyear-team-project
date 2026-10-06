import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

export interface QuoteResponse {
  symbol: string;
  price: number;
  changePercent: number;
}

@Component({
  selector: 'app-market-info-card',
  templateUrl: './market-info-card.component.html',
  styleUrls: ['./market-info-card.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule]  
})
export class MarketInfoCardComponent {
  @Input() title: string = '';
  @Input() content: string = '';
  @Input() data: QuoteResponse[] = [];

  getColorClass(): string {
    if (this.title === 'TOP GAINERS') {
      return 'top-gainers';
    } else if (this.title === 'TOP LOSERS') {
      return 'top-losers';
    } else {
      return 'watchlist';
    }
  }

  getChangeClass(changePercent: number): string {
    if (changePercent > 0) {
      return 'positive-change';
    } else if (changePercent < 0) {
      return 'negative-change';
    } else {
      return 'neutral-change';
    }
  }
}
