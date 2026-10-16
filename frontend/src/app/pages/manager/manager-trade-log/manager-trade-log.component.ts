import { Component } from '@angular/core';
import { TradeLogTableComponent } from '../../../components/manager/trade-log/trade-log-table/trade-log-table.component';

@Component({
  selector: 'app-manager-trade-log',
  standalone: true,
  imports: [TradeLogTableComponent],
  templateUrl: './manager-trade-log.component.html',
  styleUrls: ['./manager-trade-log.component.scss']
})
export class ManagerTradeLogComponent {}
