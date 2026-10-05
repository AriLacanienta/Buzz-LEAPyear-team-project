export interface CurrentHoldingsResponse {
    symbol: string;
    quantity: number;
    averagePrice: number;
    value: number;
    orderType: string;
    currencyType: string;
}