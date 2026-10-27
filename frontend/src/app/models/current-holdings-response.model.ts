export interface CurrentHoldingsResponse {
    instrumentName: string;
    instrumentSymbol: string;
    quantity: number;
    currentPrice: number;
    currencyCode: string;
    totalValue: number;
    usdValue: number;
    change: number;
    changePercent: number;
    totalCost: number;
}