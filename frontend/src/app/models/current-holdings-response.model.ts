export interface CurrentHoldingsResponse {
    instrumentName: string;
    instrumentSymbol: string;
    quantity: number;
    currentPrice: number;
    totalValue: number;
    change: number;
    changePercent: number;
    totalCost: number;
}