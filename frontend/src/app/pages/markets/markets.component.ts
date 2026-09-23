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
  displayedColumns: string[] = ['instrumentSymbol', 'instrumentName', 'currentPrice', 'change', 'changePercent', 'volume', 'marketCap'];

  constructor(private instrumentService: InstrumentService) {
    console.log('MarketsComponent initialized');
  }

  // fetch data after component initialization
  ngOnInit(): void {
    this.instrumentService.getInstruments().subscribe({
      next: (response: any) => {
        this.instruments = response.content;
        console.log('Instruments loaded:', this.instruments);
      },
      error: (error) => {
        console.error('Error loading instruments:', error);
      }
    });
  }

  formatChangePercent(value: number): string {
    const isPositive = value >= 0;
    const prefix = isPositive ? '+' : '';
    return `${prefix}${value}`;
  }

  getChangePercentClass(value: number): string {
    const isPositive = value >= 0;
    return isPositive ? 'positive-change' : 'negative-change';
  }

  getSymbolFirstLetter(symbol: string): string {
    return symbol.charAt(0).toUpperCase();
  }
}
