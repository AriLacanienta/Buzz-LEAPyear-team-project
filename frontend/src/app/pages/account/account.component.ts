import { Component } from '@angular/core';

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent {
  accountInfo = {
    name: 'Joanna Smith',
    email: 'joanna@example.com',
    phone: '+1 (555) 123-4567',
    accountType: 'Premium',
    joinDate: 'January 15, 2023',
    accountBalance: '$125,432.50'
  };
}
