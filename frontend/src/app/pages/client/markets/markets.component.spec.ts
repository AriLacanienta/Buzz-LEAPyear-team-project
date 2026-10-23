import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { InstrumentService } from '@app/services/instrument.service';
import { WatchlistService } from '@app/services/watchlist.service';
import { MarketsComponent } from './markets.component';
import { MarketInstrumentSummary } from '@app/components/client/markets/instrument-details-dialog/instrument-details-dialog.component';

describe('MarketsComponent instrument details', () => {
  let fixture: ComponentFixture<MarketsComponent>;
  let component: MarketsComponent;
  let instruments: jasmine.SpyObj<InstrumentService>;
  let watchlist: jasmine.SpyObj<WatchlistService>;

  const instrument: MarketInstrumentSummary = {
    instrumentSymbol: 'ACME',
    instrumentName: 'Acme Corporation',
    currentPrice: 125.5,
    change: 2.5,
    changePercent: 2
  };
  const quote = {
    symbol: 'ACME', price: 126, changePercent: 2.4, change: 3,
    openPrice: 124, highPrice: 127, lowPrice: 123,
    previousClosePrice: 123, volume: 1000, marketCap: 5000000,
    timestamp: '2026-10-23T12:00:00'
  };
  const details = {
    instrumentName: 'Acme Corporation', instrumentSymbol: 'ACME',
    assetType: 'EQUITY', currencyCode: 'USD'
  };

  beforeEach(async () => {
    instruments = jasmine.createSpyObj('InstrumentService', ['getInstruments', 'getQuote', 'getInstrumentDetails']);
    instruments.getInstruments.and.returnValue(of({ content: [instrument] }));
    instruments.getQuote.and.returnValue(of(quote));
    instruments.getInstrumentDetails.and.returnValue(of(details));
    watchlist = jasmine.createSpyObj('WatchlistService', ['getWatchlist', 'isInWatchlist', 'add', 'remove']);
    watchlist.getWatchlist.and.returnValue(of([]));
    watchlist.isInWatchlist.and.returnValue(false);
    watchlist.add.and.returnValue(of({ symbol: 'ACME' } as any));
    watchlist.remove.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [MarketsComponent],
      providers: [
        { provide: InstrumentService, useValue: instruments },
        { provide: WatchlistService, useValue: watchlist }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MarketsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads quote and metadata when an instrument is selected', () => {
    component.onInstrumentSelected(instrument);

    expect(instruments.getQuote).toHaveBeenCalledOnceWith('ACME');
    expect(instruments.getInstrumentDetails).toHaveBeenCalledOnceWith('ACME');
    expect(component.selectedQuote()).toEqual(quote);
    expect(component.selectedInstrumentDetails()).toEqual(details);
    expect(component.isDetailsLoading()).toBeFalse();
    expect(component.hasDetailsError()).toBeFalse();
  });

  it('retains fallback row data and reports partial quote errors', () => {
    instruments.getQuote.and.returnValue(throwError(() => new Error('quote unavailable')));
    component.onInstrumentSelected(instrument);

    expect(component.selectedInstrument()).toEqual(instrument);
    expect(component.selectedQuote()).toBeNull();
    expect(component.selectedInstrumentDetails()).toEqual(details);
    expect(component.hasDetailsError()).toBeTrue();
    expect(component.isDetailsLoading()).toBeFalse();
  });

  it('dismisses the selected instrument on Escape', () => {
    component.onInstrumentSelected(instrument);
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(component.selectedInstrument()).toBeNull();
  });

  it('adds and removes the selected instrument from favorites through the watchlist', () => {
    component.onInstrumentSelected(instrument);
    component.toggleSelectedInstrumentFavorite();
    expect(watchlist.add).toHaveBeenCalledOnceWith('ACME');
    expect(component.isFavoriteActionPending()).toBeFalse();

    watchlist.isInWatchlist.and.returnValue(true);
    component.toggleSelectedInstrumentFavorite();
    expect(watchlist.remove).toHaveBeenCalledOnceWith('ACME');
  });
});
