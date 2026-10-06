import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

export interface QuoteResponse {
  symbol: string;
  price: number;
  changePercent: number;
  name: string;
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

  getSymbolFirstLetter(symbol: string): string {
    return symbol.charAt(0).toUpperCase();
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

  getAvatarClass(): string {
    if (this.title === 'TOP GAINERS') {
      return 'avatar-gainer';
    } else if (this.title === 'TOP LOSERS') {
      return 'avatar-loser';
    } else {
      return '';
    }
  }

  formatChangePercent(changePercent: number): string {
    const sign = changePercent > 0 ? '+' : '';
    const arrow = changePercent > 0 ? '↗' : changePercent < 0 ? '↘' : '';
    return `${sign}${changePercent.toFixed(2)}%${arrow}`;
  }

  getSymbolColor(symbol: string): string {
    if (this.title === 'TOP GAINERS') {
      return 'color-green';
    } else if (this.title === 'TOP LOSERS') {
      return 'color-red';
    } else {
      return 'color-text';
    }
  }
}
