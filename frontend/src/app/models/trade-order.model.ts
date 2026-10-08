export type OrderSide = 'BUY' | 'SELL';
export type OrderStatus = 'SUBMITTED' | 'ACCEPTED' | 'VALIDATED' | 'FILLED' | 'REJECTED';

export interface TradeOrderPreviewRequest {
    side: OrderSide;
    instrumentSymbol: string;
    quantity: number;
    accountId: string | number;
}

export interface TradeOrderRequest extends TradeOrderPreviewRequest {
    price: number;
}

export interface TradeOrderPreviewResponse {
    symbol: string;
    side: OrderSide;
    quantity: number;
    livePrice: number | null;
    estimatedValue: number | null;
    cashAvailable: number;
    cashAfter: number;
    holdingQuantity: number;
    holdingQuantityAfter: number;
    averageCostAfter: number | null;
    realizedPnL: number | null;
    valid: boolean;
    reason: string | null;
}

export interface TradeOrderResponse {
    orderId: number;
    status: OrderStatus;
    statusMessage: string;
}

export interface OrderStatusResponse {
    orderId: number;
    accountId: number;
    status: OrderStatus;
    reasonText: string | null;
    timeUpdated: string;
}