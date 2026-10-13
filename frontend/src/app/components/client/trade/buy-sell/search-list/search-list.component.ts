import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SearchListItemComponent } from '../search-list-item/search-list-item.component';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';

@Component({
  selector: 'app-search-list',
  standalone: true,
  imports: [CommonModule, SearchListItemComponent],
  templateUrl: './search-list.component.html',
  styleUrl: './search-list.component.scss'
})
export class SearchListComponent {
  @Input() results: InstrumentSearchResponse[] = [];
  @Output() itemSelected = new EventEmitter<InstrumentSearchResponse>();

  selectItem(item: InstrumentSearchResponse): void {
    this.itemSelected.emit(item);
  }
}
