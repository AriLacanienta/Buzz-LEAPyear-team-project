import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface SearchResult {
  symbol: string;
  name: string;
  price: number;
  change: number;
}

@Component({
  selector: 'app-search-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-list-item.component.html',
  styleUrl: './search-list-item.component.scss'
})
export class SearchListItemComponent {
  @Input() item!: SearchResult;
}
