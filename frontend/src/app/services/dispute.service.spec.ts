import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../environments/environment';
import { DisputeService } from './dispute.service';

describe('DisputeService', () => {
  let service: DisputeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DisputeService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('resolves the selected dispute with a PATCH body and returns the saved text', () => {
    const saved = jasmine.createSpy('saved');
    service.resolve(7, 'Reviewed and corrected').subscribe(saved);
    const request = http.expectOne(`${environment.apiUrl}/dispute/7/resolve`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ resolutionMessage: 'Reviewed and corrected' });
    request.flush({ id: 7, status: 'RESOLVED', resolutionMessage: 'Reviewed and corrected' });
    expect(saved).toHaveBeenCalledWith(jasmine.objectContaining({ resolutionMessage: 'Reviewed and corrected' }));
  });

  it('surfaces resolution failures instead of returning a success result', () => {
    const success = jasmine.createSpy('success');
    const failure = jasmine.createSpy('failure');
    service.resolve(7, 'Reviewed').subscribe({ next: success, error: failure });
    http.expectOne(`${environment.apiUrl}/dispute/7/resolve`)
      .flush({}, { status: 403, statusText: 'Forbidden' });
    expect(success).not.toHaveBeenCalled();
    expect(failure).toHaveBeenCalled();
  });

  it('sends every supported query parameter without losing zeros or decimal strings', () => {
    service.search({
      page: 0, size: 100, sortBy: 'tradeValue', sortDirection: 'asc',
      accountId: '12', fromDate: '2026-10-22T00:00', toDate: '2026-10-23T00:00',
      minTradeValue: '0', maxTradeValue: '12345.67', status: 'OPEN'
    }).subscribe();
    const request = http.expectOne(req => req.url === `${environment.apiUrl}/dispute/search`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('100');
    expect(request.request.params.get('sortBy')).toBe('tradeValue');
    expect(request.request.params.get('status')).toBe('OPEN');
    expect(request.request.params.get('sortDirection')).toBe('asc');
    expect(request.request.params.get('accountId')).toBe('12');
    expect(request.request.params.get('fromDate')).toBe('2026-10-22T00:00');
    expect(request.request.params.get('toDate')).toBe('2026-10-23T00:00');
    expect(request.request.params.get('minTradeValue')).toBe('0');
    expect(request.request.params.get('maxTradeValue')).toBe('12345.67');
    request.flush({ content: [], number: 0, size: 100, totalElements: 0, totalPages: 0 });
  });

  it('omits cleared optional filters and surfaces failures', () => {
    let failed = false;
    service.search({ page: 0, size: 20, sortBy: 'createdAt', sortDirection: 'desc', accountId: '', status: '' })
      .subscribe({ error: () => failed = true });
    const request = http.expectOne(req => req.url.endsWith('/dispute/search'));
    expect(request.request.params.has('accountId')).toBeFalse();
    expect(request.request.params.has('status')).toBeFalse();
    expect(request.request.params.has('fromDate')).toBeFalse();
    request.flush({}, { status: 500, statusText: 'Error' });
    expect(failed).toBeTrue();
  });
});
