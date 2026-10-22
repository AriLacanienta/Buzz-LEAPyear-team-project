import { TradeOrderPreviewResponse } from './trade-order.model';
import { PortfolioSummaryResponse } from './portfolio-summary-response.model';
import { DisputeCard } from './dispute.model';

export interface MarketMover {
  symbol: string;
  price: number;
  changePercent: number;
}

export interface TopMoversResponse {
  topGainers: MarketMover[];
  topLosers: MarketMover[];
}

export interface ComparisonQuote {
  symbol: string;
  price: number;
  changePercent: number;
}

export interface StockComparisonResponse {
  first: ComparisonQuote;
  second: ComparisonQuote;
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  intent: string;
  message: string;
  data: unknown;
  action: string | null;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  preview?: TradeOrderPreviewResponse;
  portfolio?: PortfolioSummaryResponse;
  marketMovers?: TopMoversResponse;
  comparison?: StockComparisonResponse;
  dispute?: DisputeCard;
  action?: string | null;
}