enum OrderSide {
  BUY = 'BUY',
  SELL = 'SELL'
}

enum OrderStatus {
  SUBMITTED = 'SUBMITTED',
  ACCEPTED = 'ACCEPTED',
  VALIDATED = 'VALIDATED',
  FILLED = 'FILLED',
  REJECTED = 'REJECTED'
}

export interface RecentTradeOrderResponse {
  orderId: number;
  instrumentSymbol: string;
  side: OrderSide;
  quantity: number;
  price: number;
  orderDate: string | Date;
  status: OrderStatus | null;
  timeUpdated: string | Date | null;
  reasonText: string | null;
}

export interface SpringPage<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}