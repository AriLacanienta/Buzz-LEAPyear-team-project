import { RecentTradeOrderResponse } from './recent-trade-order-response.model';

export interface CreateDisputeRequest {
  tradeOrderId: number;
  message: string;
}

export interface DisputeResponse {
  id: number;
  tradeOrderId: number;
  accountId: number;
  tradeValue: number;
  message: string | null;
  status: 'OPEN' | 'RESOLVED';
  severity: 'NORMAL' | 'CRITICAL';
  createdAt: string;
  resolvedAt: string | null;
}

export interface DisputeCard {
  state: 'draft' | 'review' | 'submitting' | 'succeeded' | 'cancelled';
  selectedOrder?: RecentTradeOrderResponse;
  orders: RecentTradeOrderResponse[];
  ordersLoading: boolean;
  ordersError: string;
  pageIndex: number;
  totalPages: number;
  pageOrderCount: number;
  description: string;
  error: string;
  result?: DisputeResponse;
}