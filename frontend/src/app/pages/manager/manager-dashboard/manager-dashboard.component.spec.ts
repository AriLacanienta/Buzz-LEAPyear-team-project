import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject, of, throwError } from 'rxjs';
import { ManagerDashboardComponent } from './manager-dashboard.component';
import { ManagerDashboardService } from '../../../services/manager-dashboard.service';
import { ManagerDashboardSummary } from '../../../models/manager-dashboard.model';
import { DashboardInfoCardComponent } from '../../../components/manager/dashboard/dashboard-info-card/dashboard-info-card.component';

describe('ManagerDashboardComponent', () => {
  let fixture: ComponentFixture<ManagerDashboardComponent>;
  let service: jasmine.SpyObj<ManagerDashboardService>;
  const summary: ManagerDashboardSummary = {
    businessDate: '2026-10-22', timeZone: 'UTC', asOf: '2026-10-22T12:00:00Z',
    ordersToday: 3, ordersYesterday: 2, ordersDifference: 1, ordersChangePercent: 50,
    filledOrdersToday: 2, fillRatePercent: 66.7, openDisputes: 5,
    criticalOpenDisputes: 2, activeClientsToday: 2,
    assetsMovedByCurrency: [
      { currencyCode: 'EUR', totalValue: 75 },
      { currencyCode: 'USD', totalValue: 250 }
    ]
  };

  beforeEach(() => {
    service = jasmine.createSpyObj<ManagerDashboardService>('ManagerDashboardService', ['getSummary']);
    TestBed.configureTestingModule({
      imports: [ManagerDashboardComponent, NoopAnimationsModule],
      providers: [{ provide: ManagerDashboardService, useValue: service }]
    });
    fixture = TestBed.createComponent(ManagerDashboardComponent);
  });

  it('renders all cards with live values and separate currencies', fakeAsync(() => {
    service.getSummary.and.returnValue(of(summary));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    ['ORDERS TODAY', 'FILL RATE', 'OPEN DISPUTES', 'ACTIVE CLIENTS', 'ASSETS MOVED',
      '+1 (+50.0%) vs yesterday', '66.7%', '2 filled', '2 critical', 'Today',
      'Executed Today', 'EUR', '75.00', 'USD', '250.00'].forEach(value => expect(text).toContain(value));
    expect(text).not.toContain('GROSS REVENUE');
    fixture.destroy();
  }));

  it('shows loading placeholders, avoids overlapping requests and cancels on destroy', fakeAsync(() => {
    const pending = new Subject<ManagerDashboardSummary>();
    service.getSummary.and.returnValue(pending);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading dashboard');
    expect(fixture.componentInstance.summary).toBeNull();
    tick(60000);
    expect(service.getSummary).toHaveBeenCalledTimes(1);
    fixture.destroy();
    expect(pending.observed).toBeFalse();
    tick(30000);
    expect(service.getSummary).toHaveBeenCalledTimes(1);
  }));

  it('refreshes every 30 seconds and tears down its timer', fakeAsync(() => {
    service.getSummary.and.returnValue(of(summary));
    fixture.detectChanges();
    tick();
    expect(service.getSummary).toHaveBeenCalledTimes(1);
    tick(30000);
    expect(service.getSummary).toHaveBeenCalledTimes(2);
    fixture.destroy();
    tick(30000);
    expect(service.getSummary).toHaveBeenCalledTimes(2);
  }));

  it('shows initial failure and allows manual retry', fakeAsync(() => {
    service.getSummary.and.returnValues(throwError(() => new Error('offline')), of(summary));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unable to load dashboard');
    expect(fixture.componentInstance.summary).toBeNull();
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.summary).toEqual(summary);
    expect(fixture.componentInstance.error).toBeFalse();
    fixture.destroy();
  }));

  it('marks previous data stale and recovers on the next poll', fakeAsync(() => {
    service.getSummary.and.returnValues(of(summary), throwError(() => new Error('offline')), of(summary));
    fixture.detectChanges();
    tick();
    tick(30000);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('stale');
    expect(fixture.componentInstance.summary).toEqual(summary);
    tick(30000);
    fixture.detectChanges();
    expect(fixture.componentInstance.error).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('stale');
    fixture.destroy();
  }));

  it('formats undefined, negative and zero baselines without inventing percentages', fakeAsync(() => {
    service.getSummary.and.returnValue(of({
      ...summary, ordersDifference: 3, ordersChangePercent: null,
      fillRatePercent: 0, assetsMovedByCurrency: []
    }));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('+3 (N/A) vs yesterday');
    expect(fixture.nativeElement.textContent).toContain('0.0%');
    expect(fixture.nativeElement.textContent).toContain('No executions');
    fixture.componentInstance.summary = { ...summary, ordersDifference: -2, ordersChangePercent: -50 };
    expect(fixture.componentInstance.ordersComparison).toBe('-2 (-50.0%) vs yesterday');
    fixture.componentInstance.summary = { ...summary, ordersDifference: 0, ordersChangePercent: 0 };
    expect(fixture.componentInstance.ordersComparison).toBe('0 (0.0%) vs yesterday');
    fixture.destroy();
  }));

  it('keeps grouped dispute counts in the correct severity color', () => {
    const card = new DashboardInfoCardComponent();
    card.data = '1,000';
    expect(card.getOpenDisputesColor()).toBe('#ef4444');
  });

  it('fits multiple currency rows without clipping at the browser viewport width', fakeAsync(() => {
    service.getSummary.and.returnValue(of({
      ...summary, assetsMovedByCurrency: [
        { currencyCode: 'EUR', totalValue: 123456789.12 },
        { currencyCode: 'JPY', totalValue: 90000 },
        { currencyCode: 'USD', totalValue: 250 }
      ]
    }));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    const grid: HTMLElement = fixture.nativeElement.querySelector('.dashboard-info-cards');
    const card: HTMLElement = grid.querySelectorAll('mat-card')[4] as HTMLElement;
    expect(card.scrollHeight).toBeLessThanOrEqual(card.clientHeight + 1);
    expect(card.scrollWidth).toBeLessThanOrEqual(card.clientWidth + 1);
    expect(card.querySelectorAll('.currency-total').length).toBe(3);
    expect(card.textContent).toContain('JPY');
    const columns = getComputedStyle(grid).gridTemplateColumns.split(' ').length;
    expect(columns).toBe(window.innerWidth <= 650 ? 1 : window.innerWidth <= 1100 ? 2 : 5);
    fixture.destroy();
  }));
});
