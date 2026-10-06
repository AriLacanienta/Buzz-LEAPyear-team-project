enum OrderSide {
  BUY = 'BUY',
  SELL = 'SELL'
}

enum OrderStatus {
  SUBMITTED = 'SUBMITTED',
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
  status: OrderStatus;
  timeUpdated: string | Date;
  reasonText: string;
}