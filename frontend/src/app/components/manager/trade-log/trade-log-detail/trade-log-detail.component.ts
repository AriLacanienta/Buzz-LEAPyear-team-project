import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { ManagerTradeDetail } from '../../../../models/manager-trade-log.model';

@Component({
  selector: 'app-trade-log-detail',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, NgFor, NgIf],
  templateUrl: './trade-log-detail.component.html',
  styleUrls: ['./trade-log-detail.component.scss']
})
export class TradeLogDetailComponent {
  @Input() detail: ManagerTradeDetail | null = null;
  @Input() loading = false;
  @Input() error = false;
  @Output() closed = new EventEmitter<void>();
}