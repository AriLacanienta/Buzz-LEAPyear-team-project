export type AnalystOrderSide = 'BUY' | 'SELL';
export type AnalystOrderStatus = 'SUBMITTED' | 'ACCEPTED' | 'VALIDATED' | 'FILLED' | 'REJECTED';

export interface AnalystTrade {
  orderId: number;
  accountId: number;
  accountName: string;
  clientName: string;
  instrumentSymbol: string;
  assetType: string;
  currencyCode: string;
  side: AnalystOrderSide;
  quantity: number;
  price: number;
  tradeValue: number;
  orderDate: string;
  status: AnalystOrderStatus | null;
  timeUpdated: string | null;
  reasonText: string | null;
}

export interface AnalystTradePage {
  content: AnalystTrade[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
