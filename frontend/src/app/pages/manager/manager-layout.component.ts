import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-manager-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header>
      <nav>
        <strong>Admin Workspace</strong>
        <a routerLink="/manager" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
        <a routerLink="/manager/trade-log" routerLinkActive="active">Trade Log</a>
        <a routerLink="/manager/disputes" routerLinkActive="active">Disputes</a>
        <button type="button" (click)="logout()">Logout</button>
      </nav>
    </header>
    <router-outlet></router-outlet>
  `
})
export class ManagerLayoutComponent {
  constructor(private authService: AuthService) {}

  logout(): void {
    this.authService.logout();
    window.location.href = '/login';
  }
}