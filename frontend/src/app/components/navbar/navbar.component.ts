import { Component } from '@angular/core';
import { AccountService } from '../../services/account.service';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  accountName$ = this.accountService.accountName$.pipe(
    map(name => name.split(' ')[0])
  );

  constructor(private accountService: AccountService) { }
}
