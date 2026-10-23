/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InstrumentDetailsDialogComponent } from './instrument-details-dialog.component';

describe('InstrumentDetailsDialogComponent', () => {
  let fixture: ComponentFixture<InstrumentDetailsDialogComponent>;
  let component: InstrumentDetailsDialogComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [InstrumentDetailsDialogComponent] }).compileComponents();
    fixture = TestBed.createComponent(InstrumentDetailsDialogComponent);
    component = fixture.componentInstance;
    component.instrument = {
      instrumentName: 'Acme Corporation', instrumentSymbol: 'ACME',
      currentPrice: 125.5, change: 2.5, changePercent: 2
    };
    component.quote = {
      symbol: 'ACME', price: 126, changePercent: 2.4, change: 3,
      openPrice: 124, highPrice: 127, lowPrice: 123,
      previousClosePrice: 123, volume: 1000, marketCap: 5000000,
      timestamp: '2026-10-23T12:00:00'
    };
    component.details = {
      instrumentName: 'Acme Corporation', instrumentSymbol: 'ACME',
      assetType: 'EQUITY', currencyCode: 'USD'
    };
    fixture.detectChanges();
  });

  it('renders available quote and instrument details', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Acme Corporation');
    expect(text).toContain('ACME');
    expect(text).toContain('EQUITY');
    expect(text).toContain('DAY HIGH');
    expect(text).toContain('MARKET CAP');
    expect(text).toContain('USD');
    expect(text).toContain('$126.00');
  });

  it('emits close when the dialog close button is pressed', () => {
    const closed = jasmine.createSpy('closed');
    component.closed.subscribe(closed);

    fixture.nativeElement.querySelector('.close-button').click();

    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('emits favorite toggles and exposes the favorite state accessibly', () => {
    const favoriteToggled = jasmine.createSpy('favoriteToggled');
    component.favoriteToggled.subscribe(favoriteToggled);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.favorite-button');

    expect(button.getAttribute('aria-pressed')).toBe('false');
    button.click();
    expect(favoriteToggled).toHaveBeenCalledTimes(1);

    component.isFavorite = true;
    fixture.detectChanges();
    expect(button.getAttribute('aria-pressed')).toBe('true');
    expect(button.getAttribute('aria-label')).toContain('Remove ACME');
  });

  it('uses the market row as a fallback while quote details are unavailable', () => {
    component.quote = null;
    component.details = null;
    fixture.detectChanges();

    expect(component.price).toBe(125.5);
    expect(component.change).toBe(2.5);
    expect(fixture.nativeElement.textContent).toContain('Acme Corporation');
  });
});
