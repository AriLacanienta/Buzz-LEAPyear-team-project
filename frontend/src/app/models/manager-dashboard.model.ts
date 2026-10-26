export interface ManagerDashboardSummary {
  businessDate: string;
  timeZone: string;
  asOf: string;
  ordersToday: number;
  ordersYesterday: number;
  ordersDifference: number;
  ordersChangePercent: number | null;
  filledOrdersToday: number;
  fillRatePercent: number;
  openDisputes: number;
  criticalOpenDisputes: number;
  activeClientsToday: number;
  assetsMovedByCurrency: { currencyCode: string; totalValue: number }[];
}
