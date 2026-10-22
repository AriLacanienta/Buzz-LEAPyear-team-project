import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ManagerTradeLog } from '../../../../models/manager-trade-log.model';

@Component({
  selector: 'app-client-trade-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './client-trade-history.component.html',
  styleUrls: ['./client-trade-history.component.scss']
})
export class ClientTradeHistoryComponent {
  @Input() trades: ManagerTradeLog[] = [];

  trackByTradeId(_index: number, trade: ManagerTradeLog): number {
    return trade.tradeId;
  }

  statusClass(status: string | null): string {
    return `status-${(status ?? 'unknown').toLowerCase()}`;
  }
}