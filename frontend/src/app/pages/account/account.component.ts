import { Component, OnInit } from '@angular/core';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  accountInfo = {
    name: 'Joanna Smith',
    email: 'joanna@example.com',
    phone: '+1 (555) 123-4567',
    accountType: 'Premium',
    joinDate: 'January 15, 2023',
    accountBalance: '$125,432.50'
  };

  constructor(private accountService: AccountService) { }

  ngOnInit() {
    this.accountService.getMyAccount().subscribe(account => {
      this.accountInfo.name = account.accountName;
      this.accountService.setAccountName(account.accountName);
    });
  }
}
