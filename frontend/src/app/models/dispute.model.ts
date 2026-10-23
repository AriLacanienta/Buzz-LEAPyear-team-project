export interface Dispute {
  id: number;
  tradeOrderId: number;
  clientName: string;
  accountId: number;
  tradeValue: number;
  message: string | null;
  status: 'OPEN' | 'RESOLVED';
  severity: 'NORMAL' | 'CRITICAL';
  createdAt: string;
  resolvedAt: string | null;
  resolutionMessage: string | null;
}

export interface DisputePage {
  content: Dispute[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DisputeSearch {
  status?: Dispute['status'] | '';
  page: number;
  size: number;
  sortBy: 'createdAt' | 'status' | 'tradeValue';
  sortDirection: 'asc' | 'desc';
  accountId?: string;
  fromDate?: string;
  toDate?: string;
  minTradeValue?: string;
  maxTradeValue?: string;
}
