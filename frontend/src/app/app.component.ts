import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AccountService } from './services/account.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  showNavbar = true;

  constructor(private accountService: AccountService, private router: Router) {
    console.log('AppComponent initialized');
  }

  ngOnInit() {
    this.accountService.setAccountName('Joanna Smith');
    
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.showNavbar = !event.urlAfterRedirects.includes('/login') && !event.urlAfterRedirects.includes('/register');
      });
 
    this.showNavbar = !this.router.url.includes('/login') && !this.router.url.includes('/register');
  }
}
