import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SharedService {
  private accountNameSubject = new BehaviorSubject<string>('');
  accountName$ = this.accountNameSubject.asObservable();

  setAccountName(name: string) {
    this.accountNameSubject.next(name);
  }

  getAccountName() {
    return this.accountNameSubject.value;
  }
}
