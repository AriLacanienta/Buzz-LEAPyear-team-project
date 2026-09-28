import { Component, OnInit } from '@angular/core';
import { InstrumentService } from '@app/services/instrument.service';
import { MatTableModule } from '@angular/material/table';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss']
})
export class MarketsComponent implements OnInit {
  instruments: any[] = [];
  displayedColumns: string[] = ['instrumentSymbol', 'instrumentName', 'currentPrice', 'changePercent', 'volume', 'marketCap'];

  constructor(private instrumentService: InstrumentService) {
    console.log('MarketsComponent initialized');
  }

  ngOnInit(): void {
    this.instrumentService.getInstruments('EQUITY', 0, 100).subscribe({
      next: (response: any) => {
        console.log('Full response:', response);
        console.log('Response content:', response.content);
        this.instruments = response.content || [];
        console.log('Instruments loaded:', this.instruments);
        console.log('Instruments length:', this.instruments.length);
      },
      error: (error) => {
        console.error('Error loading instruments:', error);
        console.error('Error message:', error.message);
        console.error('Error status:', error.status);
      }
    });
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
    return price.toFixed(2);
  }

  formatValue(volume: number): string {
    if (volume >= 1_000_000_000) {
      return (volume / 1_000_000_000) + 'B';
    } else if (volume >= 1_000_000) {
      return (volume / 1_000_000) + 'M';
    } else if (volume >= 1_000) {
      return (volume / 1_000) + 'K';
    } else {
      return volume.toString();
    }
  }
}
