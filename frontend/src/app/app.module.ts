import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppComponent } from './app.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { AccountComponent } from './pages/account/account.component';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { managerRoutes } from './manager.routes';
import { clientRoutes } from './client.routes';
import { PortfolioHoldingCardComponent } from './components/client/portfolio/portfolio-holding-card/portfolio-holding-card.component';
import { HoldingService } from './services/holding.service';
import { HighchartsChartModule } from 'highcharts-angular';
import { FundsComponent } from './components/trade/funds/funds.component';
import { AdminComponent } from './pages/admin/admin.component';
import { authGuard } from './guards/auth.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  ...managerRoutes,
  ...clientRoutes,
  { path: 'admin', component: AdminComponent, canActivate: [authGuard], data: { roles: ['MANAGER'] } },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    AccountComponent,
    AdminComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    FormsModule,
    CommonModule,
    HttpClientModule,
    RouterModule.forRoot(routes),
    MatTableModule,
    MatIconModule,
    PortfolioHoldingCardComponent,
    FundsComponent,
    HighchartsChartModule
  ],
  providers: [
    { provide: HoldingService, useClass: HoldingService },
    // Register HTTP interceptor to add JWT token to all requests
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
