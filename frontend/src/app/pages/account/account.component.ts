import { Component, OnInit } from '@angular/core';
import { AccountService, UserProfile } from '../../services/account.service';
import { AuthService } from '../../services/auth.service';
import { CurrentAccountResponse } from '@app/models/current-account-response.model';
import { Router } from '@angular/router';
import { COUNTRY_OPTIONS } from '../../utils/country-options';

type SettingsSection = 'profile' | 'security' | 'notifications' | 'preferences' | 'billing';

interface Preferences {
  [key: string]: boolean | string;
  emailAlerts: boolean;
  pushAlerts: boolean;
  smsAlerts: boolean;
  orderFills: boolean;
  priceAlerts: boolean;
  darkMode: boolean;
  compactView: boolean;
  showPnl: boolean;
  twoFactor: boolean;
  biometrics: boolean;
  baseCurrency: string;
  timeZone: string;
  profilePicture: string;
}

type TogglePreferenceKey = 'emailAlerts' | 'pushAlerts' | 'smsAlerts' | 'orderFills'
  | 'priceAlerts' | 'darkMode' | 'compactView' | 'showPnl' | 'twoFactor' | 'biometrics';

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {
  userProfile: UserProfile | null = null;
  accountInfo: CurrentAccountResponse | null = null;
  loading = true;
  errorMessage = '';
  phoneNumber = '';
  country = '';
  readonly profilePictures = [
    { path: 'assets/images/space_avatar_astronaut.svg', label: 'Astronaut avatar' },
    { path: 'assets/images/space_avatar_planet.svg', label: 'Ringed planet avatar' },
    { path: 'assets/images/space_avatar_rocket.svg', label: 'Rocket avatar' }
  ];
  currentPassword = '';
  newPassword = '';
  activeSection: SettingsSection = 'profile';
  savedMessage = '';
  showLogoutDialog = false;
  readonly sections: { key: SettingsSection; label: string; icon: string }[] = [
    { key: 'profile', label: 'Profile', icon: 'person' },
    { key: 'security', label: 'Security', icon: 'lock' },
    { key: 'notifications', label: 'Notifications', icon: 'notifications' },
    { key: 'preferences', label: 'Preferences', icon: 'tune' },
    { key: 'billing', label: 'Billing', icon: 'credit_card' }
  ];
  preferences: Preferences = {
    emailAlerts: true, pushAlerts: false, smsAlerts: false, orderFills: true,
    priceAlerts: true, darkMode: true, compactView: false,
    showPnl: true, twoFactor: true, biometrics: false,
    baseCurrency: 'USD', timeZone: 'America/New_York',
    profilePicture: 'assets/images/space_avatar_astronaut.svg'
  };
  readonly countryOptions = COUNTRY_OPTIONS;
  readonly notificationSettings: { key: TogglePreferenceKey; label: string; description: string }[] = [
    { key: 'emailAlerts', label: 'Email alerts', description: 'Receive account and trade summaries by email.' },
    { key: 'pushAlerts', label: 'Push notifications', description: 'Get instant alerts on your device.' },
    { key: 'smsAlerts', label: 'SMS alerts', description: 'Receive critical alerts by text message.' },
    { key: 'orderFills', label: 'Order fills', description: 'Know when orders are executed or rejected.' },
    { key: 'priceAlerts', label: 'Price alerts', description: 'Be notified when watched assets hit a target.' },
  ];

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
    this.loadUserProfile();
  }

  setSection(section: SettingsSection): void {
    this.activeSection = section;
    this.savedMessage = '';
  }

  togglePreference(key: TogglePreferenceKey): void {
    this.preferences[key] = !this.preferences[key];
  }

  saveChanges(): void {
    this.errorMessage = '';
    this.accountService.updateMySettings({
      phoneNumber: this.phoneNumber,
      country: this.country,
      preferences: this.preferences
    }).subscribe({
      next: settings => {
        this.phoneNumber = settings.phoneNumber ?? '';
        this.country = settings.country ?? '';
        this.preferences = { ...this.preferences, ...settings.preferences } as Preferences;
        this.accountService.setAccountAvatar(this.preferences.profilePicture);
        this.savedMessage = 'Changes saved.';
        window.setTimeout(() => this.savedMessage = '', 3000);
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not save your changes.';
      }
    });
  }

  updatePassword(): void {
    this.errorMessage = '';
    this.savedMessage = '';
    this.accountService.changeMyPassword({
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.currentPassword = '';
        this.newPassword = '';
        this.savedMessage = 'Password updated.';
        window.setTimeout(() => this.savedMessage = '', 3000);
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not update your password.';
      }
    });
  }

  openLogoutDialog(): void {
    this.showLogoutDialog = true;
  }

  cancelLogout(): void {
    this.showLogoutDialog = false;
  }

  confirmLogout(): void {
    this.showLogoutDialog = false;
    this.authService.logout();
    this.router.navigate(['/login']);
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
    this.accountService.getMyAccount().subscribe(account => {
      this.accountInfo = account;
    });
    this.accountService.getMySettings().subscribe({
      next: settings => {
        this.phoneNumber = settings.phoneNumber ?? '';
        this.country = settings.country ?? '';
        this.preferences = { ...this.preferences, ...settings.preferences } as Preferences;
        this.accountService.setAccountAvatar(this.preferences.profilePicture);
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not load your saved settings.';
      }
    });
  }
}
