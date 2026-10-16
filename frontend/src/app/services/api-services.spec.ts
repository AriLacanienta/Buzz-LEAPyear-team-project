import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AccountService } from './account.service';
import { HoldingService } from './holding.service';
import { InstrumentService } from './instrument.service';
import { RecentOrderService } from './recent-order.service';
import { TradeOrderService } from './trade-order.service';
import { UserService } from './user.service';

describe('API-consuming services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        AccountService,
        HoldingService,
        InstrumentService,
        RecentOrderService,
        TradeOrderService,
        UserService
      ]
    });
    http = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('posts login credentials and stores the returned token and roles', () => {
    const service = TestBed.inject(AuthService);
    let response: unknown;

    service.login('user@example.com', 'secret').subscribe(value => response = value);
    const request = http.expectOne('/api/v1/auth/login');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'user@example.com', password: 'secret' });
    request.flush({
      token: 'jwt-token',
      type: 'Bearer',
      username: 'user',
      email: 'user@example.com',
      roles: ['CLIENT']
    });

    expect(response).toEqual(jasmine.objectContaining({ token: 'jwt-token' }));
    expect(localStorage.getItem('jwt_token')).toBe('jwt-token');
    expect(localStorage.getItem('auth_roles')).toBe(JSON.stringify(['CLIENT']));
  });

  it('registers users using the documented payload and stores authentication state', () => {
    const service = TestBed.inject(AuthService);

    service.register('user', 'user@example.com', 'First', 'Last', 'secret').subscribe();
    const request = http.expectOne('/api/v1/auth/register');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      username: 'user',
      email: 'user@example.com',
      firstName: 'First',
      lastName: 'Last',
      password: 'secret'
    });
    request.flush({ token: 'registered-token', roles: ['CLIENT'] });

    expect(localStorage.getItem('jwt_token')).toBe('registered-token');
  });

  it('validates the stored token with a bearer authorization header', () => {
    const service = TestBed.inject(AuthService);
    service.storeToken('jwt-token');

    service.validateToken().subscribe();
    const request = http.expectOne('/api/v1/auth/validate');

    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush({ valid: true, data: 'user' });
  });

  it('loads the current account and account balance using the documented routes', () => {
    const service = TestBed.inject(AccountService);
    service.getMyAccount().subscribe();
    http.expectOne('/api/v1/accounts/me').flush({ accountId: 7, accountName: 'Trading' });

    service.getAccountBalance('7').subscribe();
    const balanceRequest = http.expectOne('/api/v1/accounts/7');

    expect(balanceRequest.request.method).toBe('GET');
    balanceRequest.flush({ cashAvailable: 100, cashReserved: 5, portfolioValue: 200 });
  });

  it('requests paged account trade orders with page and size parameters', () => {
    const service = TestBed.inject(AccountService);
    service.getRecentTradeOrders(7, 2, 9).subscribe();
    const request = http.expectOne('/api/v1/accounts/7/tradeorders?page=2&size=9');

    expect(request.request.method).toBe('GET');
    request.flush({ content: [], number: 2, size: 9, totalElements: 0, totalPages: 0 });
  });

  it('loads account holdings and preserves the legacy all-holdings helper path', () => {
    const service = TestBed.inject(HoldingService);
    service.getHoldingsByAccountId(7).subscribe();
    http.expectOne('/api/v1/holdings/7').flush([]);

    service.getAllHoldings().subscribe();
    const request = http.expectOne('/api/v1/holdings/holdings');

    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('searches and filters instrument data from Spring Page responses', () => {
    const service = TestBed.inject(InstrumentService);
    let searchResults: unknown;
    service.searchInstruments('apple').subscribe(value => searchResults = value);
    const searchRequest = http.expectOne(request =>
      request.url === '/api/v1/quote/search' && request.params.get('q') === 'apple'
    );

    expect(searchRequest.request.method).toBe('GET');
    searchRequest.flush({ content: [{ instrumentSymbol: 'AAPL' }] });
    expect(searchResults).toEqual([{ instrumentSymbol: 'AAPL' }]);

    service.getInstruments('EQUITY', 'US', 1, 25).subscribe();
    const marketRequest = http.expectOne(request =>
      request.url === '/api/v1/quote/market' &&
      request.params.get('assetType') === 'EQUITY' &&
      request.params.get('market') === 'US' &&
      request.params.get('page') === '1' &&
      request.params.get('size') === '25'
    );
    marketRequest.flush({ content: [] });
  });

  it('uses the two documented recent-order and trade-order contracts', () => {
    const recentOrderService = TestBed.inject(RecentOrderService);
    recentOrderService.getRecentOrdersByAccountId(7, 20).subscribe();
    const recentRequest = http.expectOne('/api/v1/tradeorders/accounts/7/orders?limit=20');
    expect(recentRequest.request.method).toBe('GET');
    recentRequest.flush([]);

    const tradeOrderService = TestBed.inject(TradeOrderService);
    const preview = {
      side: 'BUY' as const,
      instrumentSymbol: 'AAPL',
      quantity: 2,
      accountId: 7
    };
    tradeOrderService.previewOrder(preview).subscribe();
    const previewRequest = http.expectOne('/api/v1/tradeorders/preview');
    expect(previewRequest.request.body).toEqual(preview);
    previewRequest.flush({ valid: true });

    tradeOrderService.getOrderStatus(42).subscribe();
    const statusRequest = http.expectOne('/api/v1/tradeorders/42');
    expect(statusRequest.request.method).toBe('GET');
    statusRequest.flush({ orderId: 42, status: 'FILLED' });
  });

  it('loads all users through the manager-facing user endpoint', () => {
    const service = TestBed.inject(UserService);
    service.getAllUsers().subscribe();
    const request = http.expectOne('/api/v1/user/allUsers');

    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
