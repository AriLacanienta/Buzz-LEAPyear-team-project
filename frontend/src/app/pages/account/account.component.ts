import { Component, OnInit } from '@angular/core';
import { AccountService, UserProfile } from '../../services/account.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  userProfile: UserProfile | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private accountService: AccountService,
    private authService: AuthService
  ) { }

  ngOnInit() {
    this.loadUserProfile();
  }

  //Loading user profile from backend
  //Extracting username from JWT token and fetches profile data
  loadUserProfile() {
    this.loading = true;
    this.errorMessage = '';

    try {
      // Extract username from the JWT token
      const token = this.authService.getToken();
      if (!token) {
        this.errorMessage = 'No authentication token found. Please log in again.';
        this.loading = false;
        return;
      }

      //Decodes JWT token to get username
      const parts = token.split('.');
      if (parts.length !== 3) {
        this.errorMessage = 'Invalid token format';
        this.loading = false;
        return;
      }

      const payload = JSON.parse(atob(parts[1]));
      //sub is the username claim from the JWT payload
      const username = payload.sub; 

      if (!username) {
        this.errorMessage = 'Username not found in token';
        this.loading = false;
        return;
      }

      //Fetching user profile from backend
      this.accountService.getUserProfile(username).subscribe(
        (profile: UserProfile) => {
          this.userProfile = profile;
          this.loading = false;
          this.accountService.setAccountName(`${profile.firstName} ${profile.lastName}`);
        },
        (error) => {
          this.loading = false;
          this.errorMessage = error.error?.message || 'Failed to load user profile';
          console.error('Error loading user profile:', error);
        }
      );
    } catch (error) {
      this.loading = false;
      this.errorMessage = 'Error processing token';
      console.error('Error:', error);
    }
  }
}
