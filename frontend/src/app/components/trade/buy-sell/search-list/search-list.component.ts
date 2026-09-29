import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SearchListItemComponent } from '../search-list-item/search-list-item.component';

export interface SearchResult {
  symbol: string;
  name: string;
  price: number;
  change: number;
}

@Component({
  selector: 'app-search-list',
  standalone: true,
  imports: [CommonModule, SearchListItemComponent],
  templateUrl: './search-list.component.html',
  styleUrl: './search-list.component.scss'
})
export class SearchListComponent {
  @Input() results: SearchResult[] = [];
  @Output() itemSelected = new EventEmitter<SearchResult>();

  selectItem(item: SearchResult): void {
    this.itemSelected.emit(item);
  }
}
