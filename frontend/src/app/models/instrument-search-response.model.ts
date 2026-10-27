export interface InstrumentSearchResponse {
    instrumentSymbol: string;
    instrumentName: string;
    currentPrice: number;
    percentChange: number;
    assetType: string;
    currencyCode: string;
    tradingSession: string;
    marketOpen: boolean;
    wholeSharesAllowed: boolean;
    fractionalAllowed: boolean;
    quantityPrecision: number;
    minOrderAmount: number;
}