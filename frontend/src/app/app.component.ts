import { Component, OnInit } from '@angular/core';
import { AccountService } from './services/account.service';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  constructor(
    private accountService: AccountService,
    private authService: AuthService
  ) {
    console.log('AppComponent initialized');
  }

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.accountService.getMyAccount().subscribe(account => {
        this.accountService.setAccountName(account.accountName);
      });
    }
  }
}
