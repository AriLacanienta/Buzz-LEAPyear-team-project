import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { AccountService } from '../../../services/account.service';
import { MarketStatusService } from '../../../services/market-status.service';
import { AuthService } from '../../../services/auth.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {
  accountName = this.accountService.accountName;

  isMarketOpen$: Observable<boolean> = this.marketStatusService.isMarketOpen();
  formattedDate$: Observable<string> = this.marketStatusService.getFormattedDate();

  marketStatusDisplay$ = this.isMarketOpen$.pipe(
    map(isOpen => ({
      text: isOpen ? 'Markets Open' : 'Markets Closed',
      class: isOpen ? 'open' : 'closed'
    }))
  );

  constructor(
    private accountService: AccountService,
    private marketStatusService: MarketStatusService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    const token = this.authService.getToken();
    if (!token) {
      return;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const username = payload.sub;
      if (!username) {
        return;
      }

      this.accountService.getUserProfile(username).subscribe(profile => {
        this.accountService.setAccountName(`${profile.firstName} ${profile.lastName}`);
      });
    } catch {
      // Keep the existing account label when the token cannot be read.
    }
  }

  getAppNameColor(word: String): string {
    return word.toLowerCase() === 'buzz' ? 'white' : '#3B82F6';
  }
}
