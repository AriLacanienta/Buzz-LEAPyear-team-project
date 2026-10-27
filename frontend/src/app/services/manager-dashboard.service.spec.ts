import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../environments/environment';
import { ManagerDashboardService } from './manager-dashboard.service';

describe('ManagerDashboardService', () => {
  let service: ManagerDashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ManagerDashboardService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the summary through the configured API URL', () => {
    service.getSummary().subscribe();
    const request = http.expectOne(`${environment.apiUrl}/dispute/summary`);
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('surfaces HTTP errors rather than emitting zero metrics', () => {
    let failed = false;
    service.getSummary().subscribe({ error: () => failed = true });
    http.expectOne(`${environment.apiUrl}/dispute/summary`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(failed).toBeTrue();
  });
});
