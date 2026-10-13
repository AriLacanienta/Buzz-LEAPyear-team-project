import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '@environments/environment';

//user profile response from the backend
export interface UserProfile {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private accountNameSubject = new BehaviorSubject<string>('');
  accountName$ = this.accountNameSubject.asObservable();

  private apiUrl = `${environment.apiUrl}/user`;

  constructor(private http: HttpClient) { }

  setAccountName(name: string) {
    this.accountNameSubject.next(name);
  }

  getAccountName() {
    return this.accountNameSubject.value;
  }
  /**
   * Fetching user profile by username
   * @param username The username to fetch profile for
   * @returns Observable with user profile data (email, firstName, lastName, username)
   */
  getUserProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/profile/${username}`);
  }
}
