import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { TradeLogTableComponent } from '../../../components/manager/trade-log/trade-log-table/trade-log-table.component';
import { TradeLogDetailComponent } from '../../../components/manager/trade-log/trade-log-detail/trade-log-detail.component';
import { ManagerTradeDetail } from '../../../models/manager-trade-log.model';
import { ManagerTradeLogService } from '../../../services/manager-trade-log.service';

@Component({
  selector: 'app-manager-trade-log',
  standalone: true,
  imports: [NgIf, TradeLogTableComponent, TradeLogDetailComponent],
  templateUrl: './manager-trade-log.component.html',
  styleUrls: ['./manager-trade-log.component.scss']
})
export class ManagerTradeLogComponent {
  selectedTradeId: number | null = null;
  selectedTrade: ManagerTradeDetail | null = null;
  detailLoading = false;
  detailError = false;

  constructor(private readonly tradeLogService: ManagerTradeLogService) {}

  selectTrade(tradeId: number): void {
    this.selectedTradeId = tradeId;
    this.selectedTrade = null;
    this.detailLoading = true;
    this.detailError = false;

    this.tradeLogService.getTradeDetail(tradeId).subscribe({
      next: detail => {
        this.selectedTrade = detail;
        this.detailLoading = false;
      },
      error: () => {
        this.detailLoading = false;
        this.detailError = true;
      }
    });
  }

  clearSelection(): void {
    this.selectedTradeId = null;
    this.selectedTrade = null;
    this.detailLoading = false;
    this.detailError = false;
  }
}
