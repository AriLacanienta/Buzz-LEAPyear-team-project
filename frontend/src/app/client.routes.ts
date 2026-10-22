import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { ClientLayoutComponent } from './pages/client/client-layout.component';
import { DashboardComponent } from './pages/client/dashboard/dashboard.component';
import { TradeComponent } from './pages/client/trade/trade.component';
import { MarketsComponent } from './pages/client/markets/markets.component';
import { AccountComponent } from './pages/account/account.component';

export const clientRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    component: ClientLayoutComponent,
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'account', component: AccountComponent },
      { path: 'portfolio', component: DashboardComponent },
      { path: 'trade', component: TradeComponent },
      { path: 'markets', component: MarketsComponent }
    ]
  }
];
