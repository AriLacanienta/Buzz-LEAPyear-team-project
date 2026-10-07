export interface RecentOrder {
  tradeId: number;
  tradeDate: string;
  clientId: number;
  clientName: string;
  instrument: string;
  assetClass: string;
  side: string;
  quantity: number;
  price: number;
  currency: string;
  value: number;
  [key: string]: any;
}