import { Component } from '@angular/core';
import { AccountService } from '../../services/account.service';
import { MarketStatusService } from '../../services/market-status.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  accountName$ = this.accountService.accountName$.pipe(
    map(name => name.split(' ')[0])
  );

  isMarketOpen$: Observable<boolean> = this.marketStatusService.isMarketOpen();
  formattedDate$: Observable<string> = this.marketStatusService.getFormattedDate();

  marketStatusDisplay$ = this.isMarketOpen$.pipe(
    map(isOpen => ({
      text: isOpen ? 'Markets Open' : 'Markets Closed',
      class: isOpen ? 'open' : 'closed'
    }))
  );

  constructor(private accountService: AccountService, private marketStatusService: MarketStatusService) { }
}
