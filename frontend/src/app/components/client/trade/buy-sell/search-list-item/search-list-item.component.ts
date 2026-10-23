import { Component, Input } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

@Component({
  selector: 'app-search-list-item',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './search-list-item.component.html',
  styleUrl: './search-list-item.component.scss'
})
export class SearchListItemComponent {
  @Input() item!: InstrumentSearchResponse;
}
