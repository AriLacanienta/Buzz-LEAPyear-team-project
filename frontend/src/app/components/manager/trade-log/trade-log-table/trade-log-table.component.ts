import { Component, OnInit } from '@angular/core';
import { NgIf, NgFor, DatePipe, CurrencyPipe } from '@angular/common';
import { ManagerTradeLog } from '../../../../models/manager-trade-log.model';
import { ManagerTradeLogService } from '../../../../services/manager-trade-log.service';

@Component({
    selector: 'app-trade-log-table',
    standalone: true,
    imports: [NgIf, NgFor, DatePipe, CurrencyPipe],
    templateUrl: './trade-log-table.component.html',
    styleUrl: './trade-log-table.component.scss'
})
export class TradeLogTableComponent implements OnInit {
    trades: ManagerTradeLog[] = [];
    pageIndex = 0;
    pageSize = 25;
    totalElements = 0;
    totalPages = 0;
    loading = false;
    error = false;

  constructor(private service: ManagerTradeLogService) {}

    ngOnInit(): void {
        this.loadTrades();
    }

    loadTrades(): void {
        this.loading = true;
    this.error = false;

    this.service.getTradeLog(this.pageIndex, this.pageSize).subscribe({
    next: page => {
        this.trades = page.content;
        this.pageIndex = page.number;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
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
        this.loadTrades();
        }
    }

    nextPage(): void {
        if (this.pageIndex + 1 < this.totalPages) {
        this.pageIndex++;
        this.loadTrades();
        }
    }

    trackByTradeId(_: number, trade: ManagerTradeLog): number {
        return trade.tradeId;
    }
}