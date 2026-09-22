import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule, Routes } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { TradeComponent } from './pages/trade/trade.component';
import { MarketsComponent } from './pages/markets/markets.component';
import { AccountComponent } from './pages/account/account.component';
import { NavbarComponent } from './components/navbar/navbar.component';

const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'account', component: AccountComponent },
  { path: 'portfolio', component: DashboardComponent },
  { path: 'trade', component: TradeComponent },
  { path: 'markets', component: MarketsComponent }
];

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    TradeComponent,
    MarketsComponent,
    AccountComponent,
    NavbarComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    RouterModule.forRoot(routes),
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }