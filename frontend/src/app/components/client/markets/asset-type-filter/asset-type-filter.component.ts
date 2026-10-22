import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-asset-type-filter',
  templateUrl: './asset-type-filter.component.html',
  styleUrls: ['./asset-type-filter.component.scss'],
  imports: [CommonModule],
  standalone: true
})
export class AssetTypeFilterComponent {
  @Input() assetTypes: string[] = [];
  @Input() selectedAssetType: string = '';
  @Output() assetTypeChange = new EventEmitter<string>();

  onAssetTypeSelect(assetType: string): void {
    this.assetTypeChange.emit(assetType);
  }
}
