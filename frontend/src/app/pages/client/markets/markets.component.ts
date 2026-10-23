import { Component, OnInit, OnDestroy, computed, signal } from '@angular/core';
import { InstrumentService } from '@app/services/instrument.service';
import { WatchlistService } from '@app/services/watchlist.service';
import { Subscription } from 'rxjs';
import { InstrumentsTableComponent } from '@app/components/client//markets/instruments-table/instruments-table.component';
import { AssetTypeFilterComponent } from '@app/components/client//markets/asset-type-filter/asset-type-filter.component';
import { MarketTabsComponent } from '@app/components/client//markets/market-tabs/market-tabs.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss'],
  imports: [CommonModule, InstrumentsTableComponent, AssetTypeFilterComponent, MarketTabsComponent],
  standalone: true
})
export class MarketsComponent implements OnInit, OnDestroy {
  instruments = signal<any[]>([]);
  searchTerm = signal('');
  visibleInstruments = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();
    if (!query) return this.instruments();

    return this.instruments().filter((instrument) =>
      `${instrument.instrumentSymbol ?? ''} ${instrument.instrumentName ?? ''}`
        .toLowerCase()
        .includes(query)
    );
  });
  displayedColumns: string[] = ['watchlist', 'instrumentSymbol', 'instrumentName', 'currentPrice', 'changePercent', 'volume', 'marketCap'];
  selectedMarket = signal('US');
  selectedAssetType = signal('EQUITY');
  markets: string[] = ['US', 'UK', 'India'];
  assetTypes: string[] = ['EQUITY', 'BOND', 'CRYPTO', 'ETF'];
  private refreshSubscription: Subscription | null = null;

  constructor(
    private instrumentService: InstrumentService,
    private watchlistService: WatchlistService
  ) {
    console.log('MarketsComponent initialized');
  }

  ngOnInit(): void {
    this.loadInstruments();
    this.watchlistService.getWatchlist().subscribe({
      error: (error) => console.error('Error loading watchlist:', error)
    });
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
      assetType: this.selectedAssetType(),
      market: this.selectedMarket(),
      page: 0,
      size: 100
    });
    
    this.instrumentService.getInstruments(
      this.selectedAssetType(),
      this.selectedMarket(),
      0,
      100
    ).subscribe(
      (response: any) => {
        console.log('Received response:', response);
        this.instruments.set(response.content || []);
        console.log('Instruments count:', this.instruments().length);
      },
      (error) => {
        console.error('Error loading instruments:', error);
      }
    );
  }

  onMarketChange(market: string): void {
    this.selectedMarket.set(market);
    this.loadInstruments();
    this.startAutoRefresh();
  }

  onAssetTypeChange(assetType: string): void {
    this.selectedAssetType.set(assetType);
    this.loadInstruments();
    this.startAutoRefresh();
  }

  onSearch(searchTerm: string): void {
    this.searchTerm.set(searchTerm);
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }
}

