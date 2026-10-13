import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppComponent } from './app.component';
import { DashboardComponent } from './pages/client/dashboard/dashboard.component';
import { TradeComponent } from './pages/client/trade/trade.component';
import { MarketsComponent } from './pages/client/markets/markets.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { authGuard } from './guards/auth.guard';
import { AccountComponent } from './pages/account/account.component';
import { NavbarComponent } from './components/client//navbar/navbar.component';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { PortfolioHoldingCardComponent } from './components/client//portfolio/portfolio-holding-card/portfolio-holding-card.component';
import { ManagerDashboardComponent } from './pages/manager/manager-dashboard/manager-dashboard.component';
import { ManagerDisputesComponent } from './pages/manager/manager-disputes/manager-disputes.component';
import { ManagerTradeLogComponent } from './pages/manager/manager-trade-log/manager-trade-log.component';
import { managerRoutes } from './manager.routes';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'account', component: AccountComponent, canActivate: [authGuard] },
  { path: 'portfolio', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'trade', component: TradeComponent, canActivate: [authGuard] },
  { path: 'markets', component: MarketsComponent, canActivate: [authGuard]},
  ...managerRoutes,
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    AccountComponent,
    NavbarComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    CommonModule,
    HttpClientModule,
    RouterModule.forRoot(routes),
    MatTableModule,
    MatIconModule,
    PortfolioHoldingCardComponent,
    MarketsComponent,
    DashboardComponent,
    ManagerDashboardComponent,
    ManagerDisputesComponent,
    ManagerTradeLogComponent
  ],
  providers: [
    // Register HTTP interceptor to add JWT token to all requests
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }