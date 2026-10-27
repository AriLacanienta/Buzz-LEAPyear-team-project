import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WatchlistService } from '@app/services/watchlist.service';
import { InstrumentsTableComponent } from './instruments-table.component';

describe('InstrumentsTableComponent row selection', () => {
  let fixture: ComponentFixture<InstrumentsTableComponent>;
  let component: InstrumentsTableComponent;
  let watchlist: jasmine.SpyObj<WatchlistService>;
  const instrument = {
    instrumentSymbol: 'ACME', instrumentName: 'Acme Corporation',
    currentPrice: 100, change: 1, changePercent: 1, volume: 1000, marketCap: 500000
  };

  beforeEach(async () => {
    watchlist = jasmine.createSpyObj('WatchlistService', ['isInWatchlist', 'add', 'remove']);
    watchlist.isInWatchlist.and.returnValue(false);
    watchlist.add.and.returnValue({ subscribe: () => {} } as any);
    watchlist.remove.and.returnValue({ subscribe: () => {} } as any);
    await TestBed.configureTestingModule({
      imports: [InstrumentsTableComponent],
      providers: [{ provide: WatchlistService, useValue: watchlist }]
    }).compileComponents();
    fixture = TestBed.createComponent(InstrumentsTableComponent);
    component = fixture.componentInstance;
    component.instruments = [instrument];
    fixture.detectChanges();
  });

  it('emits the selected instrument when a row is clicked or activated by keyboard', () => {
    const selected = jasmine.createSpy('selected');
    component.instrumentSelected.subscribe(selected);
    const row: HTMLElement = fixture.nativeElement.querySelector('tr.mat-mdc-row');

    row.click();
    row.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    row.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));

    expect(selected).toHaveBeenCalledTimes(3);
    expect(selected).toHaveBeenCalledWith(instrument);
  });

  it('does not open row details from the watchlist button or its keyboard event', () => {
    const selected = jasmine.createSpy('selected');
    component.instrumentSelected.subscribe(selected);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.watchlist-button');

    button.click();
    button.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();

    expect(selected).not.toHaveBeenCalled();
    expect(watchlist.add).toHaveBeenCalledWith('ACME');
  });
});
