import { Component, Output, EventEmitter, OnInit, DestroyRef } from '@angular/core';
import { BehaviorSubject, catchError, debounceTime, distinctUntilChanged, of, switchMap, timer, map, EMPTY, combineLatest, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InstrumentService } from '@app/services/instrument.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchListComponent } from '../search-list/search-list.component';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule, SearchListComponent],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss'
})
export class SearchComponent implements OnInit {
  @Output() itemSelected = new EventEmitter<InstrumentSearchResponse | null>();

  private searchTerm$ = new BehaviorSubject<string>('');
  private focused$ = new BehaviorSubject<boolean>(false);
  searchInput: string = '';
  searchResults: InstrumentSearchResponse[] = [];
  isSearchBoxFocused: boolean = false;
  isItemSelected: boolean = false;

  constructor(private instrumentService: InstrumentService, private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    const term$ = this.searchTerm$.pipe(
      map(term => term.trim()),
      debounceTime(500),
      distinctUntilChanged()
    );

    combineLatest([this.focused$, term$]).pipe(
      switchMap(([focused, term]) => 
        focused ? timer (0, POLL_INTERVAL_MS).pipe(switchMap(() => this.fetchResults(term))) : EMPTY
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(results => this.searchResults = results);
  }

  private fetchResults(term: string): Observable<InstrumentSearchResponse[]> {
    const request$: Observable<InstrumentSearchResponse[]> = term === '' ? of([]) : this.instrumentService.searchInstruments(term);

    return request$.pipe(
      catchError(() => of(this.searchResults))
    );
  }

  onSearchBoxFocus(): void {
    this.isSearchBoxFocused = true;
    this.focused$.next(true);
  }

  onSearchBoxBlur(): void {
    this.isSearchBoxFocused = false;
    this.focused$.next(false);
    this.searchResults = [];
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchInput = input.value;
    this.searchTerm$.next(this.searchInput);
  }

  onSearchClear(): void {
    this.isItemSelected = false;
    this.itemSelected.emit(null);
    this.searchInput = '';
    this.searchResults = [];
    this.isSearchBoxFocused = false;
  }

  onResultSelected(item: InstrumentSearchResponse): void {
    this.itemSelected.emit(item);
    this.searchInput = '';
    this.searchTerm$.next('');
    this.searchResults = [];
    this.isSearchBoxFocused = false;
    this.focused$.next(false);
    this.isItemSelected = true;
  }
}
