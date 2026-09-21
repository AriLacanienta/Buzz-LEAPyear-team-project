import { Component, OnInit } from '@angular/core';
import { AccountService } from './services/account.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  constructor(private accountService: AccountService) {
    console.log('AppComponent initialized');
  }

  ngOnInit() {
    this.accountService.setAccountName('Joanna Smith');
  }
}
