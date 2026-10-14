import { Component } from '@angular/core';
import { DashboardInfoCardComponent } from '@app/components/manager/dashboard/dashboard-info-card/dashboard-info-card.component';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [DashboardInfoCardComponent],
  templateUrl: './manager-dashboard.component.html',
  styleUrls: ['./manager-dashboard.component.scss']
})
export class ManagerDashboardComponent {

}
