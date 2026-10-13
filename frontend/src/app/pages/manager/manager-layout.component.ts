import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../components/manager/navbar/navbar.component';

@Component({
  selector: 'app-manager-layout',
  standalone: true,
  imports: [NavbarComponent, RouterOutlet],
  templateUrl: './manager-layout.component.html',
  styleUrls: ['./manager-layout.component.scss']
})
export class ManagerLayoutComponent {}