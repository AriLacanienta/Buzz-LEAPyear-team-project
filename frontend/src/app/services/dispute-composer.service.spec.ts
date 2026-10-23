import { DisputeComposerService } from './dispute-composer.service';
import { RecentTradeOrderResponse } from '../models/recent-trade-order-response.model';

describe('DisputeComposerService', () => {
  it('forwards filled-order selections but ignores non-executed orders', () => {
    const service = new DisputeComposerService();
    const selected = jasmine.createSpy('selected');
    const subscription = service.requests$.subscribe(selected);
    const order = { orderId: 7, status: 'FILLED' } as RecentTradeOrderResponse;
    service.openForOrder({ ...order, status: 'REJECTED' } as RecentTradeOrderResponse);
    service.openForOrder(order);
    expect(selected).toHaveBeenCalledOnceWith(order);
    subscription.unsubscribe();
  });
});