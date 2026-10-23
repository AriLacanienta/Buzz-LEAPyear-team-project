import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { WatchlistItem } from '@app/models/watchlist-item.model';

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {
  private readonly apiUrl = `${environment.apiUrl}/watchlist`;
  private readonly symbolsState = signal<ReadonlySet<string>>(new Set());
  readonly symbols = this.symbolsState.asReadonly();

  constructor(private http: HttpClient) {}

  getWatchlist(): Observable<WatchlistItem[]> {
    return this.http.get<WatchlistItem[]>(this.apiUrl).pipe(
      tap(items => this.setSymbols(items))
    );
  }

  add(symbol: string): Observable<WatchlistItem> {
    return this.http.post<WatchlistItem>(this.apiUrl, { symbol }).pipe(
      tap(item => this.addSymbol(item.symbol))
    );
  }

  remove(symbol: string): Observable<void> {
    const normalizedSymbol = this.normalize(symbol);
    return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(normalizedSymbol)}`).pipe(
      tap(() => this.removeSymbol(normalizedSymbol))
    );
  }

  isInWatchlist(symbol: string): boolean {
    return this.symbolsState().has(this.normalize(symbol));
  }

  private setSymbols(items: WatchlistItem[]): void {
    this.symbolsState.set(new Set(items.map(item => this.normalize(item.symbol))));
  }

  private addSymbol(symbol: string): void {
    const symbols = new Set(this.symbolsState());
    symbols.add(this.normalize(symbol));
    this.symbolsState.set(symbols);
  }

  private removeSymbol(symbol: string): void {
    const symbols = new Set(this.symbolsState());
    symbols.delete(this.normalize(symbol));
    this.symbolsState.set(symbols);
  }

  private normalize(symbol: string): string {
    return symbol.trim().toUpperCase();
  }
}
