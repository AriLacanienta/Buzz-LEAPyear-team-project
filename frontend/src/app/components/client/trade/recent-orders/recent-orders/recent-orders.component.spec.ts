import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { RecentOrdersComponent } from './recent-orders.component';
import { AccountService } from '@app/services/account.service';
import { TradeOrderService } from '@app/services/trade-order.service';
import { DisputeComposerService } from '@app/services/dispute-composer.service';
import { DisputeService } from '@app/services/dispute.service';
import { RecentTradeOrderResponse } from '@app/models/recent-trade-order-response.model';

describe('RecentOrdersComponent disputes', () => {
  let fixture: ComponentFixture<RecentOrdersComponent>;
  let composer: DisputeComposerService;
  let disputeCreated: Subject<void>;
  let getRecentTradeOrders: jasmine.Spy;
  const filledOrder = {
    orderId: 7, instrumentSymbol: 'AAPL', side: 'BUY', quantity: 5, price: 20,
    orderDate: '2026-10-22T11:00:00', status: 'FILLED', timeUpdated: null, reasonText: null,
    hasOpenDispute: false
  } as RecentTradeOrderResponse;
  const disputedOrder = { ...filledOrder, orderId: 8, hasOpenDispute: true };

  beforeEach(async () => {
    disputeCreated = new Subject<void>();
    getRecentTradeOrders = jasmine.createSpy().and.returnValue(of({
      content: [filledOrder, disputedOrder, { ...filledOrder, orderId: 9, status: 'REJECTED' }],
      number: 0, totalElements: 3, totalPages: 1
    }));
    await TestBed.configureTestingModule({
      imports: [RecentOrdersComponent],
      providers: [
        { provide: AccountService, useValue: {
          getMyAccount: () => of({ accountId: '3', accountName: 'My account' }),
          getRecentTradeOrders
        } },
        { provide: TradeOrderService, useValue: { orderPlaced$: new Subject<void>() } },
        { provide: DisputeService, useValue: { disputeCreated$: disputeCreated.asObservable() } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(RecentOrdersComponent);
    composer = TestBed.inject(DisputeComposerService);
  });

  it('shows references and emits only the selected filled order from its row action', fakeAsync(() => {
    const open = spyOn(composer, 'openForOrder');
    fixture.detectChanges();
    tick(0);
    fixture.detectChanges();
    const references = fixture.nativeElement.querySelectorAll('.order-reference');
    expect(references[0].textContent).toContain('#7');
    expect(references[1].textContent).toContain('#8');
    expect(references[2].textContent).toContain('#9');
    const buttons = fixture.nativeElement.querySelectorAll('.raise-dispute');
    expect(buttons.length).toBe(2);
    expect(buttons[0].textContent).toContain('Raise dispute');
    buttons[0].click();
    expect(open).toHaveBeenCalledOnceWith(filledOrder);
    expect(buttons[1].textContent).toContain('Pending dispute');
    expect(buttons[1].disabled).toBeTrue();
    expect(buttons[1].getAttribute('aria-label')).toContain('Pending dispute for order #8');

    disputeCreated.next();
    tick(0);
    expect(getRecentTradeOrders).toHaveBeenCalledTimes(2);
    fixture.destroy();
  }));
});