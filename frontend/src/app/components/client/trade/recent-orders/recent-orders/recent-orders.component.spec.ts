import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { RecentOrdersComponent } from './recent-orders.component';
import { AccountService } from '@app/services/account.service';
import { TradeOrderService } from '@app/services/trade-order.service';
import { DisputeComposerService } from '@app/services/dispute-composer.service';
import { RecentTradeOrderResponse } from '@app/models/recent-trade-order-response.model';

describe('RecentOrdersComponent disputes', () => {
  let fixture: ComponentFixture<RecentOrdersComponent>;
  let composer: DisputeComposerService;
  const filledOrder = {
    orderId: 7, instrumentSymbol: 'AAPL', side: 'BUY', quantity: 5, price: 20,
    orderDate: '2026-10-22T11:00:00', status: 'FILLED', timeUpdated: null, reasonText: null
  } as RecentTradeOrderResponse;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecentOrdersComponent],
      providers: [
        { provide: AccountService, useValue: {
          getMyAccount: () => of({ accountId: '3', accountName: 'My account' }),
          getRecentTradeOrders: () => of({
            content: [filledOrder, { ...filledOrder, orderId: 8, status: 'REJECTED' }],
            number: 0, totalElements: 2, totalPages: 1
          })
        } },
        { provide: TradeOrderService, useValue: { orderPlaced$: new Subject<void>() } }
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
    const buttons = fixture.nativeElement.querySelectorAll('.raise-dispute');
    expect(buttons.length).toBe(1);
    buttons[0].click();
    expect(open).toHaveBeenCalledOnceWith(filledOrder);
    fixture.destroy();
  }));
});