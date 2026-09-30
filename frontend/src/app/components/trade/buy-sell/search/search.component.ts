import { Component, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchListComponent, SearchResult } from '../search-list/search-list.component';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule, SearchListComponent],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss'
})
export class SearchComponent implements OnInit {
  searchInput: string = '';
  searchResults: SearchResult[] = [];
  isSearchBoxFocused: boolean = false;
  private allResults: SearchResult[] = [];
  
  @Output() itemSelected = new EventEmitter<SearchResult | null>();

  ngOnInit(): void {
    // TODO: Inject InstrumentService and load initial data
    this.initializeMockData();
  }

  private initializeMockData(): void {
    this.allResults = [
      {
        symbol: 'AAPL',
        name: 'Apple Inc.',
        price: 228.45,
        change: 2.34
      },
      {
        symbol: 'MSFT',
        name: 'Microsoft Corporation',
        price: 418.32,
        change: 1.61
      },
      {
        symbol: 'GOOGL',
        name: 'Alphabet Inc.',
        price: 142.89,
        change: -0.87
      },
      {
        symbol: 'TSLA',
        name: 'Tesla Inc.',
        price: 298.76,
        change: 3.45
      },
      {
        symbol: 'AMZN',
        name: 'Amazon.com Inc.',
        price: 195.87,
        change: -1.23
      },
      {
        symbol: 'NVDA',
        name: 'NVIDIA Corporation',
        price: 892.45,
        change: 5.67
      },
      {
        symbol: 'META',
        name: 'Meta Platforms Inc.',
        price: 567.23,
        change: 2.89
      },
      {
        symbol: 'AMD',
        name: 'Advanced Micro Devices',
        price: 156.78,
        change: -0.45
      },
      {
        symbol: 'NFLX',
        name: 'Netflix Inc.',
        price: 289.34,
        change: 1.12
      },
      {
        symbol: 'INTC',
        name: 'Intel Corporation',
        price: 42.56,
        change: -2.34
      }
    ];
  }

  onSearchBoxFocus(): void {
    this.isSearchBoxFocused = true;
    if (this.searchInput.trim() === '') {
      // Show all results when focused with empty input
      this.searchResults = this.allResults;
    } else {
      // Show filtered results when focused
      this.filterResults();
    }
  }

  onSearchBoxBlur(): void {
    this.isSearchBoxFocused = false;
    this.searchResults = [];
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchInput = input.value;
    if (this.isSearchBoxFocused) {
      this.filterResults();
    }
  }

  onSearchClear(): void {
    this.itemSelected.emit(null);
    this.searchInput = '';
    this.searchResults = [];
    this.isSearchBoxFocused = false;
  }

  private filterResults(): void {
    // TODO: Filter results based on searchInput
    // Call InstrumentService to get filtered results
    // This will be populated when you integrate with the service
    if (this.searchInput.trim() === '') {
      this.searchResults = this.allResults;
    } else {
      // Placeholder filtering logic
      this.searchResults = this.allResults.filter(item =>
        item.symbol.toUpperCase().includes(this.searchInput.toUpperCase()) ||
        item.name.toUpperCase().includes(this.searchInput.toUpperCase())
      );
    }
  }

  onResultSelected(item: SearchResult): void {
    this.itemSelected.emit(item);
    this.searchInput = '';
    this.searchResults = [];
    this.isSearchBoxFocused = false;
  }
}
