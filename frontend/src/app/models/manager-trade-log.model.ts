export interface ManagerTradeLog {
    tradeId: number;
    tradeDate: string;
    accountId: number;
    accountName: string;
    clientId: number;
    clientName: string;
    clientEmail: string;
    instrumentName: string;
    instrumentSymbol: string;
    assetType: string;
    currency: string;
    side: 'BUY' | 'SELL';
    quantity: number;
    price: number;
    value: number;
    status: string | null;
    statusUpdatedAt: string | null;
    statusReason: string | null;
    timePlaced: string | null;
    timeFilled: string | null;
}

export interface ManagerTradeLogPage {
    content: ManagerTradeLog[];
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}