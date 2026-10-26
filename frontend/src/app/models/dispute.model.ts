import { RecentTradeOrderResponse } from "./recent-trade-order-response.model";

export interface CreateDisputeRequest {
  tradeOrderId: number;
  message: string;
}

export interface DisputeResponse {
  id: number;
  tradeOrderId: number;
  clientName: string;
  accountId: number;
  tradeValue: number;
  message: string | null;
  status: "OPEN" | "RESOLVED";
  severity: "NORMAL" | "CRITICAL";
  createdAt: string;
  resolvedAt: string | null;
  resolutionMessage: string | null;
}

export interface DisputeCard {
  state: "draft" | "review" | "submitting" | "succeeded" | "cancelled";
  selectedOrder?: RecentTradeOrderResponse;
  orders: RecentTradeOrderResponse[];
  ordersLoading: boolean;
  ordersError: string;
  pageIndex: number;
  totalPages: number;
  pageOrderCount: number;
  description: string;
  error: string;
  result?: DisputeResponse;
}

export interface DisputePage {
  content: DisputeResponse[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DisputeSearch {
  status?: DisputeResponse["status"] | "";
  page: number;
  size: number;
  sortBy: "createdAt" | "status" | "tradeValue";
  sortDirection: "asc" | "desc";
  accountId?: string;
  userId?: number;
  fromDate?: string;
  toDate?: string;
  minTradeValue?: string;
  maxTradeValue?: string;
}
