import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SearchResult } from '../search-list/search-list.component';

@Component({
  selector: 'app-search-result-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-result-detail.component.html',
  styleUrl: './search-result-detail.component.scss'
})
export class SearchResultDetailComponent {
  @Input() selectedResult: SearchResult | null = null;
}
