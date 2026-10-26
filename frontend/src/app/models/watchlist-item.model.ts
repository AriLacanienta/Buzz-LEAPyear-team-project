export interface WatchlistItem {
  id: number;
  symbol: string;
  instrumentName: string;
  assetType: string;
  currencyCode: string;
  price: number | null;
  change: number | null;
  changePercent: number | null;
  timestamp: string | null;
}
