import { Injectable } from '@angular/core';
import { Observable, interval } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class MarketStatusService {
  isMarketOpen(): Observable<boolean> {
    return interval(60000).pipe(
      startWith(0),
      map(() => this.checkMarketOpen())
    );
  }

  private checkMarketOpen(): boolean {
    const now = new Date();
    const day = now.getDay();
    const hour = now.getHours();
    const minute = now.getMinutes();

    if (day >= 1 && day <= 5) {
      if (hour > 9 && hour < 16) {
        return true;
      }
      if (hour === 9 && minute >= 30) {
        return true;
      }
      if (hour === 16 && minute === 0) {
        return true;
      }
    }
    return false;
  }

  getFormattedDate(): Observable<string> {
    return interval(60000).pipe(
      startWith(0),
      map(() => this.formatDate())
    );
  }
  
  private formatDate(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hour = String(now.getHours()).padStart(2, '0');
    const minute = String(now.getMinutes()).padStart(2, '0');
    const second = String(now.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  }
}