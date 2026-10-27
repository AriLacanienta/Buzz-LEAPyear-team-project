import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { WatchlistService } from '@app/services/watchlist.service';

@Component({
  selector: 'app-instruments-table',
  templateUrl: './instruments-table.component.html',
  styleUrls: ['./instruments-table.component.scss'],
  standalone: true,
  imports: [CommonModule, MatTableModule]
})
export class InstrumentsTableComponent {
  @Input() instruments: any[] = [];
  @Input() selectedMarket: string = 'US';
  @Input() displayedColumns: string[] = ['watchlist', 'instrumentSymbol', 'instrumentName', 'currentPrice', 'changePercent', 'volume', 'marketCap'];
  @Output() instrumentSelected = new EventEmitter<any>();

  constructor(private watchlistService: WatchlistService) {}

  selectInstrument(instrument: any): void {
    this.instrumentSelected.emit(instrument);
  }

  onRowKeydown(event: KeyboardEvent, instrument: any): void {
    if (event.target !== event.currentTarget) return;
    if (event.key === 'Enter') this.selectInstrument(instrument);
    if (event.key === ' ') {
      event.preventDefault();
      this.selectInstrument(instrument);
    }
  }

  isInWatchlist(symbol: string): boolean {
    return this.watchlistService.isInWatchlist(symbol);
  }

  toggleWatchlist(symbol: string): void {
    if (this.isInWatchlist(symbol)) {
      this.watchlistService.remove(symbol).subscribe();
    } else {
      this.watchlistService.add(symbol).subscribe();
    }
  }

  formatChangePercent(value: number): string {
    const isPositive = value >= 0;
    const prefix = isPositive ? '+' : '';
    return `${prefix}${value}`;
  }

  getChangePercentClass(value: number): string {
    if (value > 0) {
      return 'positive-change';
    } else if (value < 0) {
      return 'negative-change';
    } else {
      return 'neutral-change';
    }
  }

  getSymbolFirstLetter(symbol: string): string {
    return symbol.charAt(0).toUpperCase();
  }

  getChangeArrow(value: number): string {
    if (value > 0) {
      return '▲';
    } else if (value < 0) {
      return '▼';
    } else {
      return '';
    }
  }

  formatPrice(price: number): string {
    switch (this.selectedMarket) {
      case 'US':
        return '$' + price.toFixed(2);
      case 'UK':
        return '£' + price.toFixed(2);
      case 'India':
        return '₹' + Math.floor(price);
      default:
        return price.toFixed(2);
    }
  }

  formatValue(value: number): string {
    if (value >= 1_000_000_000) {
      return Math.floor(value / 1_000_000_000) + 'B';
    } else if (value >= 1_000_000) {
      return Math.floor(value / 1_000_000) + 'M';
    } else if (value >= 1_000) {
      return Math.floor(value / 1_000) + 'K';
    } else {
      return value.toString();
    }
  }
}
