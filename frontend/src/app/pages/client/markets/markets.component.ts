import { Component, OnInit, OnDestroy, HostListener, computed, signal } from '@angular/core';
import { InstrumentDetailsResponse, InstrumentService, QuoteResponse } from '@app/services/instrument.service';
import { WatchlistService } from '@app/services/watchlist.service';
import { Subscription, catchError, forkJoin, of } from 'rxjs';
import { InstrumentsTableComponent } from '@app/components/client//markets/instruments-table/instruments-table.component';
import { AssetTypeFilterComponent } from '@app/components/client//markets/asset-type-filter/asset-type-filter.component';
import { MarketTabsComponent } from '@app/components/client//markets/market-tabs/market-tabs.component';
import { CommonModule } from '@angular/common';
import { InstrumentDetailsDialogComponent, MarketInstrumentSummary } from '@app/components/client/markets/instrument-details-dialog/instrument-details-dialog.component';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss'],
  imports: [CommonModule, InstrumentsTableComponent, AssetTypeFilterComponent, MarketTabsComponent, InstrumentDetailsDialogComponent],
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
  selectedInstrument = signal<MarketInstrumentSummary | null>(null);
  selectedQuote = signal<QuoteResponse | null>(null);
  selectedInstrumentDetails = signal<InstrumentDetailsResponse | null>(null);
  isDetailsLoading = signal(false);
  hasDetailsError = signal(false);
  isFavoriteActionPending = signal(false);
  favoriteActionError = signal('');
  markets: string[] = ['US', 'UK', 'India'];
  assetTypes: string[] = ['EQUITY', 'BOND', 'CRYPTO', 'ETF'];
  private refreshSubscription: Subscription | null = null;
  private detailsSubscription: Subscription | null = null;

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

  isInstrumentInWatchlist(symbol: string): boolean {
    return this.watchlistService.isInWatchlist(symbol);
  }

  toggleSelectedInstrumentFavorite(): void {
    const instrument = this.selectedInstrument();
    if (!instrument || this.isFavoriteActionPending()) return;

    this.isFavoriteActionPending.set(true);
    this.favoriteActionError.set('');
    const onSuccess = () => this.isFavoriteActionPending.set(false);
    const onError = () => {
      this.favoriteActionError.set('Could not update favorites. Please try again.');
      this.isFavoriteActionPending.set(false);
    };

    if (this.watchlistService.isInWatchlist(instrument.instrumentSymbol)) {
      this.watchlistService.remove(instrument.instrumentSymbol).subscribe({ next: onSuccess, error: onError });
    } else {
      this.watchlistService.add(instrument.instrumentSymbol).subscribe({ next: onSuccess, error: onError });
    }
  }

  onInstrumentSelected(instrument: MarketInstrumentSummary): void {
    this.detailsSubscription?.unsubscribe();
    this.selectedInstrument.set(instrument);
    this.selectedQuote.set(null);
    this.selectedInstrumentDetails.set(null);
    this.isDetailsLoading.set(true);
    this.hasDetailsError.set(false);

    let requestFailed = false;
    const onRequestError = () => {
      requestFailed = true;
      return of(null);
    };

    this.detailsSubscription = forkJoin({
      quote: this.instrumentService.getQuote(instrument.instrumentSymbol).pipe(catchError(onRequestError)),
      details: this.instrumentService.getInstrumentDetails(instrument.instrumentSymbol).pipe(catchError(onRequestError))
    }).subscribe(({ quote, details }) => {
      if (this.selectedInstrument()?.instrumentSymbol !== instrument.instrumentSymbol) return;
      this.selectedQuote.set(quote);
      this.selectedInstrumentDetails.set(details);
      this.hasDetailsError.set(requestFailed);
      this.isDetailsLoading.set(false);
    });
  }

  closeInstrumentDetails(): void {
    this.detailsSubscription?.unsubscribe();
    this.detailsSubscription = null;
    this.selectedInstrument.set(null);
    this.isDetailsLoading.set(false);
    this.favoriteActionError.set('');
  }

  @HostListener('document:keydown.escape')
  closeDetailsOnEscape(): void {
    if (this.selectedInstrument()) this.closeInstrumentDetails();
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
    this.detailsSubscription?.unsubscribe();
  }
}

