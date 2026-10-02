export interface InstrumentSearchResponse {
    instrumentSymbol: string;
    instrumentName: string;
    currentPrice: number;
    percentChange: number;
    assetType: string;
    currencyCode: string;
}