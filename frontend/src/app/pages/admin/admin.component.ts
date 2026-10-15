import { Component } from '@angular/core';
import { UserResponse, UserService } from '../../services/user.service';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent {
  users: UserResponse[] = [];
  isLoading = false;
  errorMessage = '';
  hasLoadedUsers = false;

  constructor(private userService: UserService) {}

  loadAllUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.userService.getAllUsers().subscribe({
      next: users => {
        this.users = users;
        this.hasLoadedUsers = true;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load users. Please try again.';
        this.isLoading = false;
      }
    });
  }
}
