import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { RecentTradeOrderResponse } from '../models/recent-trade-order-response.model';

/** Connects order-row actions to the chatbot without persisting client data. */
@Injectable({ providedIn: 'root' })
export class DisputeComposerService {
  private readonly requests = new Subject<RecentTradeOrderResponse>();
  readonly requests$ = this.requests.asObservable();

  openForOrder(order: RecentTradeOrderResponse): void {
    if (order.status === 'FILLED') {
      this.requests.next(order);
    }
  }
}