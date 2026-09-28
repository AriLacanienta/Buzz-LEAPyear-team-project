import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppComponent } from './app.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { TradeComponent } from './pages/trade/trade.component';
import { MarketsComponent } from './pages/markets/markets.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { authGuard } from './auth.guard';
import { AccountComponent } from './pages/account/account.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { MatTableModule } from '@angular/material/table';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'account', component: AccountComponent },
  { path: 'portfolio', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'trade', component: TradeComponent, canActivate: [authGuard] },
  { path: 'markets', component: MarketsComponent, canActivate: [authGuard]},
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    TradeComponent,
    MarketsComponent,
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
    HttpClientModule,
    MatTableModule
  ],
  providers: [
    // Register HTTP interceptor to add JWT token to all requests
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }