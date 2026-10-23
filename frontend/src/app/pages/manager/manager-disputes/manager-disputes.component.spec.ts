import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { Subject, of, throwError } from 'rxjs';
import { DisputeService } from '../../../services/dispute.service';
import { DisputePage } from '../../../models/dispute.model';
import { ManagerDisputesComponent } from './manager-disputes.component';

describe('ManagerDisputesComponent', () => {
  let fixture: ComponentFixture<ManagerDisputesComponent>;
  let service: jasmine.SpyObj<DisputeService>;
  let dialog: jasmine.SpyObj<Dialog>;
  let closed: Subject<boolean | undefined>;
  const page: DisputePage = {
    number: 0, size: 20, totalElements: 41, totalPages: 3,
    content: [{
      id: 1, clientName: 'Alex Smith', accountId: 12, tradeOrderId: 7, tradeValue: 10000,
      message: '<script>not HTML</script>', status: 'OPEN', severity: 'CRITICAL',
      createdAt: '2026-10-22T12:00:00', resolvedAt: null, resolutionMessage: null
    }]
  };

  beforeEach(() => {
    service = jasmine.createSpyObj<DisputeService>('DisputeService', ['search']);
    service.search.and.returnValue(of(page));
    closed = new Subject<boolean | undefined>();
    dialog = jasmine.createSpyObj<Dialog>('Dialog', ['open']);
    const dialogRef = jasmine.createSpyObj<DialogRef<unknown>>('DialogRef', ['close'], { closed });
    dialog.open.and.returnValue(dialogRef);
    TestBed.configureTestingModule({
      imports: [ManagerDisputesComponent], providers: [
        { provide: DisputeService, useValue: service }, { provide: Dialog, useValue: dialog }
      ]
    });
    fixture = TestBed.createComponent(ManagerDisputesComponent);
  });

  it('loads the first server page and shows six summary fields and row actions by default', () => {
    fixture.detectChanges();
    expect(service.search).toHaveBeenCalledWith({ page: 0, size: 20, sortBy: 'createdAt', sortDirection: 'desc', status: 'OPEN' });
    const text = fixture.nativeElement.textContent;
    ['41 disputes found', 'Alex Smith', 'CRITICAL', 'OPEN', 'Not resolved',
      '<script>not HTML</script>', 'Page 1 of 3'].forEach(value => expect(text).toContain(value));
    const headers = Array.from(fixture.nativeElement.querySelectorAll('thead th') as NodeListOf<HTMLElement>);
    expect(headers.map(header => header.textContent?.trim())).toEqual([
      'Dispute ID', 'Client Name', 'Status', 'Severity', 'Message', 'Resolved', 'Actions'
    ]);
    expect(fixture.nativeElement.querySelector('.dispute-summary').cells.length).toBe(7);
    expect(fixture.nativeElement.querySelector('.dispute-details').hidden).toBeTrue();
    expect(fixture.nativeElement.querySelector('dl')).toBeNull();
    expect(text).not.toContain('10,000.00');
    expect(fixture.nativeElement.querySelector('tbody script')).toBeNull();
  });

  it('embeds all user disputes with open-first sorting and no filter controls', () => {
    fixture.componentRef.setInput('userId', 42);
    fixture.detectChanges();
    expect(service.search).toHaveBeenCalledOnceWith({
      userId: 42, page: 0, size: 20, sortBy: 'status', sortDirection: 'asc'
    });
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('h1')).toBeNull();
    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('nav')).not.toBeNull();
    const component = fixture.componentInstance;
    component.changePage(1);
    expect(service.search.calls.mostRecent().args[0]).toEqual({
      userId: 42, page: 1, size: 20, sortBy: 'status', sortDirection: 'asc'
    });
    component.openResolution(page.content[0]);
    closed.next(true);
    component.retry();
    expect(service.search.calls.mostRecent().args[0]).toEqual({
      userId: 42, page: 1, size: 20, sortBy: 'status', sortDirection: 'asc'
    });
    component.clearFilters();
    expect(service.search.calls.mostRecent().args[0]).toEqual({
      userId: 42, page: 0, size: 20, sortBy: 'status', sortDirection: 'asc'
    });
  });

  it('cancels the old user request and clears stale rows and expansion on client changes', () => {
    const first = new Subject<DisputePage>();
    const second = new Subject<DisputePage>();
    service.search.and.returnValues(first, second);
    fixture.componentRef.setInput('userId', 42);
    fixture.detectChanges();
    first.next(page);
    fixture.componentInstance.toggleDispute(1);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
    fixture.componentRef.setInput('userId', 43);
    fixture.detectChanges();
    expect(first.observed).toBeFalse();
    expect(fixture.componentInstance.expandedDisputeIds.size).toBe(0);
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(service.search.calls.mostRecent().args[0]).toEqual({
      userId: 43, page: 0, size: 20, sortBy: 'status', sortDirection: 'asc'
    });
    first.next(page);
    expect(fixture.componentInstance.result).toBeNull();
    second.next({ ...page, content: [], totalElements: 0, totalPages: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No disputes for this client.');
  });

  it('expands all fields on row click and collapses using the accessible button without double toggling', () => {
    fixture.detectChanges();
    const row: HTMLTableRowElement = fixture.nativeElement.querySelector('.dispute-summary');
    const button: HTMLButtonElement = row.querySelector('button')!;
    expect(button.getAttribute('aria-expanded')).toBe('false');
    row.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('true');
    const details: HTMLTableRowElement = fixture.nativeElement.querySelector('.dispute-details');
    expect(button.getAttribute('aria-controls')).toBe(details.id);
    expect(details.hidden).toBeFalse();
    const fields = Array.from(details.querySelectorAll('dt')).map(field => field.textContent);
    expect(fields).toEqual([
      'Dispute ID', 'Client Name', 'Account ID', 'Order ID', 'Trade value',
      'Status', 'Severity', 'Created', 'Resolved', 'Message', 'Resolution message'
    ]);
    const values = Array.from(details.querySelectorAll('dd')).map(field => field.textContent);
    expect(values).toEqual([
      '1', 'Alex Smith', '12', '7', '10,000.00', 'OPEN', 'CRITICAL',
      jasmine.stringMatching(/2026/), 'Not resolved', '<script>not HTML</script>', 'No resolution message'
    ]);
    expect(details.querySelector('script')).toBeNull();
    button.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(details.hidden).toBeTrue();
    button.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('true');
  });

  for (const message of [null, '', 'Short message', 'x'.repeat(50), 'x'.repeat(51)]) {
    it(`renders the exact message preview for ${message === null ? 'null' : message.length + ' characters'} and the full text when expanded`, () => {
      service.search.and.returnValue(of({ ...page, content: [{ ...page.content[0], message }] }));
      fixture.detectChanges();
      const preview: HTMLElement = fixture.nativeElement.querySelector('.dispute-summary .message');
      expect(preview.textContent).toBe(
        message ? message.slice(0, 50) + (message.length > 50 ? '...' : '') : 'No additional details'
      );
      fixture.nativeElement.querySelector('.expand-toggle').click();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.dispute-details .full-message .message').textContent)
        .toBe(message || 'No additional details');
    });
  }

  it('expands rows independently and clears expansion when requesting new data', () => {
    service.search.and.returnValue(of({
      ...page,
      content: [...page.content, {
        ...page.content[0], id: 2, status: 'RESOLVED', resolvedAt: '2026-10-23T12:00:00'
      }]
    }));
    fixture.detectChanges();
    const rows: NodeListOf<HTMLTableRowElement> = fixture.nativeElement.querySelectorAll('.dispute-summary');
    rows[0].click();
    rows[1].click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.dispute-details dl').length).toBe(2);
    expect(rows[1].cells[5].textContent).toContain('2026');
    rows[0].click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.dispute-details dl').length).toBe(1);
    const component = fixture.componentInstance;
    for (const reload of [() => component.applyFilters(), () => component.changePage(1), () => component.retry()]) {
      component.toggleDispute(1);
      reload();
      fixture.detectChanges();
      expect(component.expandedDisputeIds.size).toBe(0);
      expect(fixture.nativeElement.querySelector('dl')).toBeNull();
    }
  });

  it('opens the selected open dispute without toggling expansion and hides resolved actions', () => {
    service.search.and.returnValue(of({
      ...page, content: [page.content[0], {
        ...page.content[0], id: 2, status: 'RESOLVED', resolutionMessage: '<b>Resolved</b>'
      }]
    }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.resolve-button').length).toBe(1);
    fixture.nativeElement.querySelector('.resolve-button').click();
    expect(dialog.open).toHaveBeenCalledWith(jasmine.any(Function), jasmine.objectContaining({
      data: 1, ariaLabelledBy: 'dispute-resolution-title', autoFocus: 'textarea'
    }));
    expect(fixture.componentInstance.expandedDisputeIds.size).toBe(0);
    fixture.componentInstance.openResolution({
      ...page.content[0], status: 'RESOLVED'
    });
    expect(dialog.open).toHaveBeenCalledTimes(1);
    const rows: NodeListOf<HTMLTableRowElement> = fixture.nativeElement.querySelectorAll('.dispute-summary');
    rows[1].click();
    fixture.detectChanges();
    const details: HTMLElement = fixture.nativeElement.querySelectorAll('.dispute-details')[1];
    expect(details.textContent).toContain('<b>Resolved</b>');
    expect(details.querySelector('b')).toBeNull();
    expect(details.querySelector('td')?.colSpan).toBe(7);
  });

  it('refreshes only on successful resolution and uses applied rather than edited filters', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.filters.patchValue({ accountId: '12', status: '', size: 10 });
    component.applyFilters();
    component.changePage(1);
    const applied = service.search.calls.mostRecent().args[0];
    component.filters.patchValue({ accountId: '999', size: 50 });
    component.openResolution(page.content[0]);
    const count = service.search.calls.count();
    closed.next(undefined);
    expect(service.search.calls.count()).toBe(count);
    closed.next(false);
    expect(service.search.calls.count()).toBe(count);
    closed.next(true);
    expect(service.search.calls.count()).toBe(count + 1);
    expect(service.search.calls.mostRecent().args[0]).toEqual(applied);
  });

  it('reloads the last valid page when resolving the last row invalidates the current page', () => {
    fixture.detectChanges();
    fixture.componentInstance.changePage(2);
    service.search.calls.reset();
    service.search.and.returnValues(
      of({ ...page, number: 2, content: [], totalElements: 40, totalPages: 2 }),
      of({ ...page, number: 1, totalElements: 40, totalPages: 2 })
    );
    fixture.componentInstance.openResolution(page.content[0]);
    closed.next(true);
    fixture.detectChanges();
    expect(service.search.calls.allArgs().map(args => args[0].page)).toEqual([2, 1]);
    expect(fixture.nativeElement.textContent).toContain('Page 2 of 2');
  });

  it('returns to page zero when resolution removes the last matching dispute', () => {
    fixture.detectChanges();
    fixture.componentInstance.changePage(1);
    service.search.calls.reset();
    service.search.and.returnValues(
      of({ ...page, number: 1, content: [], totalElements: 0, totalPages: 0 }),
      of({ ...page, number: 0, content: [], totalElements: 0, totalPages: 0 })
    );
    fixture.componentInstance.openResolution(page.content[0]);
    closed.next(true);
    fixture.detectChanges();
    expect(service.search.calls.allArgs().map(args => args[0].page)).toEqual([1, 0]);
    expect(fixture.nativeElement.textContent).toContain('Page 0 of 0');
  });

  it('paginates on the server, preserves applied filters and resets page when filters change', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.filters.patchValue({
      accountId: '12', fromDate: '2026-10-22T00:00', toDate: '2026-10-23T00:00',
      minTradeValue: '0', maxTradeValue: '10000', sortBy: 'status', sortDirection: 'asc', size: 10
    });
    component.applyFilters();
    component.changePage(1);
    expect(service.search.calls.mostRecent().args[0]).toEqual({
      ...component.filters.getRawValue(), page: 1
    });
    component.applyFilters();
    expect(service.search.calls.mostRecent().args[0].page).toBe(0);
    component.clearFilters();
    expect(service.search.calls.mostRecent().args[0]).toEqual({
      accountId: '', fromDate: '', toDate: '', minTradeValue: '', maxTradeValue: '',
      sortBy: 'createdAt', sortDirection: 'desc', size: 20, page: 0, status: 'OPEN'
    });
  });

  it('blocks invalid account, values and reversed ranges before requesting', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    for (const patch of [
      { accountId: '-1' }, { minTradeValue: '-1' },
      { minTradeValue: '20', maxTradeValue: '10' },
      { fromDate: '2026-10-23T00:00', toDate: '2026-10-22T00:00' }
    ]) {
      component.filters.reset();
      component.filters.patchValue(patch);
      component.applyFilters();
      expect(component.validationError).not.toBe('');
    }
    expect(service.search).toHaveBeenCalledTimes(1);
  });

  it('supports resolved and all status searches and restores open on clear', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.filters.controls.status.setValue('RESOLVED');
    component.applyFilters();
    component.changePage(1);
    expect(service.search.calls.mostRecent().args[0].status).toBe('RESOLVED');
    component.filters.controls.status.setValue('');
    component.applyFilters();
    expect(service.search.calls.mostRecent().args[0].status).toBe('');
    component.clearFilters();
    expect(component.filters.controls.status.value).toBe('OPEN');
    expect(service.search.calls.mostRecent().args[0].status).toBe('OPEN');
    expect(service.search.calls.mostRecent().args[0].page).toBe(0);
  });

  it('submits rendered form controls and keeps unapplied edits out of paging requests', () => {
    fixture.detectChanges();
    const minimum: HTMLInputElement = fixture.nativeElement.querySelector('[formControlName="minTradeValue"]');
    minimum.value = '0';
    minimum.dispatchEvent(new Event('input'));
    const maximum: HTMLInputElement = fixture.nativeElement.querySelector('[formControlName="maxTradeValue"]');
    maximum.value = '100.25';
    maximum.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(service.search.calls.mostRecent().args[0].minTradeValue).toBe('0');
    expect(service.search.calls.mostRecent().args[0].maxTradeValue).toBe('100.25');
    maximum.value = '999';
    maximum.dispatchEvent(new Event('input'));
    fixture.componentInstance.changePage(1);
    expect(service.search.calls.mostRecent().args[0].maxTradeValue).toBe('100.25');
    expect(service.search.calls.mostRecent().args[0].page).toBe(1);
  });

  it('cancels obsolete requests and cleans up on destroy', () => {
    const first = new Subject<DisputePage>();
    const second = new Subject<DisputePage>();
    service.search.and.returnValues(first, second);
    fixture.detectChanges();
    expect(first.observed).toBeTrue();
    fixture.componentInstance.applyFilters();
    expect(first.observed).toBeFalse();
    expect(second.observed).toBeTrue();
    first.next(page);
    expect(fixture.componentInstance.result).toBeNull();
    fixture.destroy();
    expect(second.observed).toBeFalse();
  });

  it('shows failure, retries the same query, and distinguishes empty results', () => {
    service.search.and.returnValues(throwError(() => new Error('offline')),
      of({ ...page, content: [], totalElements: 0, totalPages: 0 }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unable to load disputes');
    fixture.componentInstance.retry();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No disputes match these filters');
    expect(fixture.nativeElement.textContent).toContain('Page 0 of 0');
    const count = service.search.calls.count();
    fixture.componentInstance.changePage(1);
    fixture.componentInstance.changePage(-1);
    expect(service.search.calls.count()).toBe(count);
  });
});
