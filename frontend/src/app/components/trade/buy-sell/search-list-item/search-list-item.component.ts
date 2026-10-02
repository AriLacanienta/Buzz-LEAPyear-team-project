import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

@Component({
  selector: 'app-search-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-list-item.component.html',
  styleUrl: './search-list-item.component.scss'
})
export class SearchListItemComponent {
  @Input() item!: InstrumentSearchResponse;
}
