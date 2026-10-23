export interface PortfolioSummaryResponse {
    holdingsCount: number;
    portfolioValue: number;
    totalCostBasis: number;
    todayChange: number;
    todayChangePercent: number;
    allTimeChange: number;
    allTimeChangePercent: number;
}