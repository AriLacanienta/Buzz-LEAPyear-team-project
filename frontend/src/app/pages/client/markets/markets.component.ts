import { Component, OnInit, OnDestroy } from '@angular/core';
import { InstrumentService } from '@app/services/instrument.service';
import { Subscription } from 'rxjs';
import { InstrumentsTableComponent } from '@app/components/markets/instruments-table/instruments-table.component';
import { AssetTypeFilterComponent } from '@app/components/markets/asset-type-filter/asset-type-filter.component';
import { MarketTabsComponent } from '@app/components/markets/market-tabs/market-tabs.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss'],
  imports: [CommonModule, InstrumentsTableComponent, AssetTypeFilterComponent, MarketTabsComponent],
  standalone: true
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

