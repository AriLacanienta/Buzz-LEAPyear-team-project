import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ManagerClient } from '../../../../models/manager-client.model';
import { ClientTradeHistoryComponent } from '../client-trade-history/client-trade-history.component';

@Component({
  selector: 'app-client-detail',
  standalone: true,
  imports: [CommonModule, ClientTradeHistoryComponent],
  templateUrl: './client-detail.component.html',
  styleUrls: ['./client-detail.component.scss']
})
export class ClientDetailComponent {
  @Input() client: ManagerClient | null = null;
  @Output() refreshRequested = new EventEmitter<void>();

  get tradeValue(): number {
    return this.client?.trades.reduce((total, trade) => total + trade.value, 0) ?? 0;
  }

}