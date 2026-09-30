import { Component } from '@angular/core';
import { SearchComponent } from '../search/search.component';
import { SearchResultDetailComponent } from '../search-result-detail/search-result-detail.component';
import { SearchResult } from '../search-list/search-list.component';

@Component({
  selector: 'app-buy-sell',
  standalone: true,
  imports: [SearchComponent, SearchResultDetailComponent],
  templateUrl: './buy-sell.component.html',
  styleUrl: './buy-sell.component.scss'
})
export class BuySellComponent {
  isBuy: boolean = true;
  selectedTag: string = 'All';
  selectedResult: SearchResult | null = null;

  toggleOrderType(type: 'buy' | 'sell'): void {
    this.isBuy = type === 'buy';
  }

  selectTag(tag: string): void {
    this.selectedTag = tag;
  }

  onQuantityInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    // Remove non-numeric characters
    input.value = input.value.replace(/[^0-9]/g, '');
    // Remove leading zeros (but keep at least one digit)
    input.value = input.value.replace(/^0+(?=\d)/, '');
  }

  onSearchItemSelected(item: SearchResult | null): void {
    this.selectedResult = item;
    console.log('Selected item from search:', item);
    // Handle selected item (populate quantity, set symbol, etc.)
  }
}
