import { Component, Input } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

@Component({
  selector: 'app-search-result-detail',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './search-result-detail.component.html',
  styleUrl: './search-result-detail.component.scss'
})
export class SearchResultDetailComponent {
  @Input() selectedResult: InstrumentSearchResponse | null = null;
}
