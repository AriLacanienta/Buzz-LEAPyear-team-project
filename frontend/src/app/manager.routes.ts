import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { adminGuard } from './admin.guard';
import { ManagerLayoutComponent } from './pages/manager/manager-layout.component';
import { ManagerDashboardComponent } from './pages/manager/manager-dashboard/manager-dashboard.component';
import { ManagerDisputesComponent } from './pages/manager/manager-disputes/manager-disputes.component';
import { ManagerTradeLogComponent } from './pages/manager/manager-trade-log/manager-trade-log.component';

export const managerRoutes: Routes = [
  {
    path: 'manager',
    canActivate: [authGuard, adminGuard],
    component: ManagerLayoutComponent,
    children: [
      { path: '', component: ManagerDashboardComponent },
      { path: 'disputes', component: ManagerDisputesComponent },
      { path: 'trade-log', component: ManagerTradeLogComponent }
    ]
  }
];