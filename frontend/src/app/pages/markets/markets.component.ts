import { Component, OnInit, OnDestroy } from '@angular/core';
import { InstrumentService } from '@app/services/instrument.service';
import { MatTableModule } from '@angular/material/table';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss']
})
export class MarketsComponent implements OnInit, OnDestroy {
  instruments: any[] = [];
  displayedColumns: string[] = ['instrumentSymbol', 'instrumentName', 'currentPrice', 'changePercent', 'volume', 'marketCap'];
  selectedMarket: string = 'US';
  selectedAssetType: string = 'EQUITY';
  markets: string[] = ['US', 'UK', 'India'];
  assetTypes: string[] = ['EQUITY', 'BOND', 'CRYPTO', 'ETF'];
  private refreshSubscription: Subscription | null = null;

  constructor(private instrumentService: InstrumentService) {
    console.log('MarketsComponent initialized');
  }

  ngOnInit(): void {
    this.loadInstruments();
    this.startAutoRefresh();
  }

  private startAutoRefresh(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }

    this.refreshSubscription = new Subscription();
    const interval = setInterval(() => {
      this.loadInstruments();
    }, 5000);

    this.refreshSubscription.add(() => clearInterval(interval));
  }
  loadInstruments(): void {
    console.log('Loading instruments with:', {
      assetType: this.selectedAssetType,
      market: this.selectedMarket,
      page: 0,
      size: 100
    });
    
    this.instrumentService.getInstruments(
      this.selectedAssetType,
      this.selectedMarket,
      0,
      100
    ).subscribe(
      (response: any) => {
        console.log('Received response:', response);
        this.instruments = response.content || [];
        console.log('Instruments count:', this.instruments.length);
      },
      (error) => {
        console.error('Error loading instruments:', error);
      }
    );
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

  getCurrencyCode(market: string): string {
    const currencyMap: { [key: string]: string } = {
      'US': 'USD',
      'UK': 'GBP',
      'India': 'INR'
    };
    return currencyMap[market] || market;
  }

  onMarketChange(market: string): void {
    this.selectedMarket = market;
    this.loadInstruments();
    this.startAutoRefresh();
  }

  onAssetTypeChange(assetType: string): void {
    this.selectedAssetType = assetType;
    this.loadInstruments();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

}
