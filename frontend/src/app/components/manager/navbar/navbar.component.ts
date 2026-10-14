import { Component } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MarketStatusService } from '../../../services/market-status.service';
import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-manager-navbar',
  standalone: true,
  imports: [AsyncPipe, MatIconModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  isMarketOpen$: Observable<boolean> = this.marketStatusService.isMarketOpen();
  formattedDate$: Observable<string> = this.marketStatusService.getFormattedDate();

  marketStatusDisplay$ = this.isMarketOpen$.pipe(
    map(isOpen => ({
      text: isOpen ? 'Markets Open' : 'Markets Closed',
      class: isOpen ? 'open' : 'closed'
    }))
  );

  constructor(
    private marketStatusService: MarketStatusService,
    private authService: AuthService,
    private router: Router
  ) { }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getAppNameColor(word: string): string {
    return word.toLowerCase() === 'buzz' ? 'white' : '#3B82F6';
  }

  getPageDescription(): string {
    const route = this.router.url;
    if (route.includes('/manager/trade-log')) {
      return 'FULL TRADE AUDIT LOG - ALL ORDERS';
    } else if (route.includes('/manager/disputes')) {
      return 'DISPUTE QUEUE - REQUIRES MANAGER REVIEW';
    } else {
      return 'DAILY OPERATIONS SUMMARY';
    }
  }
}
