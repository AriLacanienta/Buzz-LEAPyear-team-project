import { CommonModule, CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { InstrumentDetailsResponse, QuoteResponse } from '@app/services/instrument.service';

export interface MarketInstrumentSummary {
  instrumentName: string;
  instrumentSymbol: string;
  currentPrice: number;
  change?: number;
  changePercent?: number;
  volume?: number;
  marketCap?: number;
}

@Component({
  selector: 'app-instrument-details-dialog',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './instrument-details-dialog.component.html',
  styleUrl: './instrument-details-dialog.component.scss'
})
export class InstrumentDetailsDialogComponent {
  @Input({ required: true }) instrument!: MarketInstrumentSummary;
  @Input() quote: QuoteResponse | null = null;
  @Input() details: InstrumentDetailsResponse | null = null;
  @Input() isLoading = false;
  @Input() hasError = false;
  @Input() isFavorite = false;
  @Input() favoritePending = false;
  @Input() favoriteError = '';
  @Output() closed = new EventEmitter<void>();
  @Output() favoriteToggled = new EventEmitter<void>();

  get currencyCode(): string {
    return this.details?.currencyCode || 'USD';
  }

  get price(): number {
    return this.quote?.price ?? this.instrument.currentPrice;
  }

  get change(): number {
    return this.quote?.change ?? this.instrument.change ?? 0;
  }

  get changePercent(): number {
    return this.quote?.changePercent ?? this.instrument.changePercent ?? 0;
  }

  get volume(): number | null {
    return this.quote?.volume ?? this.instrument.volume ?? null;
  }

  get marketCap(): number | null {
    return this.quote?.marketCap ?? this.instrument.marketCap ?? null;
  }

  close(): void {
    this.closed.emit();
  }
}
