import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NgIf, NgFor, DatePipe, CurrencyPipe } from '@angular/common';
import { ManagerTradeLog } from '../../../../models/manager-trade-log.model';
import { ManagerTradeLogService } from '../../../../services/manager-trade-log.service';

@Component({
    selector: 'app-trade-log-table',
    standalone: true,
    imports: [NgIf, NgFor, DatePipe, CurrencyPipe],
    templateUrl: './trade-log-table.component.html',
    styleUrls: ['./trade-log-table.component.scss']
})
export class TradeLogTableComponent implements OnInit {
    @Input() selectedTradeId: number | null = null;
    @Output() tradeSelected = new EventEmitter<number>();
    trades: ManagerTradeLog[] = [];
    filteredTrades: ManagerTradeLog[] = [];
    filteredTradeSet: ManagerTradeLog[] = [];
    pageIndex = 0;
    pageSize = 10;
    totalElements = 0;
    totalPages = 0;
    loading = false;
    error = false;
    searchTerm = '';
    statusFilter = 'ALL';
    sideFilter = 'ALL';

  constructor(private service: ManagerTradeLogService) {}

    ngOnInit(): void {
        this.loadTrades();
    }

    loadTrades(): void {
        this.loading = true;
    this.error = false;

    this.service.getTradeLog(0, 100).subscribe({
    next: page => {
        this.trades = page.content;
        this.pageIndex = 0;
        this.applyFilters();
        this.loading = false;
    },
    error: () => {
        this.loading = false;
        this.error = true;
    }
    });
}

    previousPage(): void {
        if (this.pageIndex > 0) {
        this.pageIndex--;
        this.updateVisiblePage();
        }
    }

    nextPage(): void {
        if (this.pageIndex + 1 < this.totalPages) {
        this.pageIndex++;
        this.updateVisiblePage();
        }
    }

    trackByTradeId(_: number, trade: ManagerTradeLog): number {
        return trade.tradeId;
    }

    selectTrade(trade: ManagerTradeLog): void {
        this.tradeSelected.emit(trade.tradeId);
    }

    private applyFilters(): void {
        const search = this.searchTerm.trim().toLowerCase();

        this.filteredTradeSet = this.trades.filter(trade => {
            const searchableText = [
                trade.tradeId,
                trade.clientName,
                trade.clientEmail,
                trade.instrumentSymbol,
                trade.instrumentName
            ].join(' ').toLowerCase();
            const matchesSearch = !search || searchableText.includes(search);
            const matchesStatus = this.statusFilter === 'ALL'
                || trade.status?.toUpperCase() === this.statusFilter;
            const matchesSide = this.sideFilter === 'ALL'
                || trade.side === this.sideFilter;

            return matchesSearch && matchesStatus && matchesSide;
        });

        this.totalElements = this.filteredTradeSet.length;
        this.totalPages = Math.ceil(this.totalElements / this.pageSize);
        this.pageIndex = Math.min(this.pageIndex, Math.max(this.totalPages - 1, 0));
        this.updateVisiblePage();
    }

    private updateVisiblePage(): void {
        const start = this.pageIndex * this.pageSize;
        this.filteredTrades = this.filteredTradeSet.slice(start, start + this.pageSize);
    }

    setSearchTerm(value: string): void {
        this.searchTerm = value;
        this.pageIndex = 0;
        this.applyFilters();
    }

    setStatusFilter(value: string): void {
        this.statusFilter = value;
        this.pageIndex = 0;
        this.applyFilters();
    }

    setSideFilter(value: string): void {
        this.sideFilter = value;
        this.pageIndex = 0;
        this.applyFilters();
    }

    clearFilters(): void {
        this.searchTerm = '';
        this.statusFilter = 'ALL';
        this.sideFilter = 'ALL';
        this.pageIndex = 0;
        this.applyFilters();
    }

    getSideColor(side: string): string {
        switch (side.toUpperCase()) {
            case 'BUY':
                return '#00D492';
            case 'SELL':
                return '#FF6467';
            default:
                return 'inherit';
        }
    }

    orderRejected(status: string | null): string {
        return status?.toUpperCase() === 'REJECTED' ? '#62748E' : 'inherit';
    }
}