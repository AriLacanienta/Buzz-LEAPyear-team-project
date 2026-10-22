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

  it('posts only the order ID and description to the existing dispute endpoint', () => {
    const result = { id: 12, tradeOrderId: 7, status: 'OPEN' };
    service.create({ tradeOrderId: 7, message: 'Incorrect Price' }).subscribe(response => {
      expect(response.id).toBe(12);
    });
    const request = http.expectOne(`${environment.apiUrl}/dispute`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ tradeOrderId: 7, message: 'Incorrect Price' });
    request.flush(result, { status: 201, statusText: 'Created' });
  });

  it('propagates conflicts without retrying the creation', () => {
    let status = 0;
    service.create({ tradeOrderId: 7, message: 'Wrong price' }).subscribe({ error: error => status = error.status });
    http.expectOne(`${environment.apiUrl}/dispute`)
      .flush({}, { status: 409, statusText: 'Conflict' });
    expect(status).toBe(409);
    http.expectNone(`${environment.apiUrl}/dispute`);
  });
});