import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ManagerClient } from '../../../../models/manager-client.model';
import { ClientTradeHistoryComponent } from '../client-trade-history/client-trade-history.component';

@Component({
  selector: 'app-client-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ClientTradeHistoryComponent],
  templateUrl: './client-detail.component.html',
  styleUrls: ['./client-detail.component.scss']
})
export class ClientDetailComponent {
  @Input() client: ManagerClient | null = null;
  @Output() refreshRequested = new EventEmitter<void>();
  @Output() notesSaved = new EventEmitter<{ clientId: number; notes: string }>();

  draftNotes = '';
  notesSavedMessage = false;

  ngOnChanges(): void {
    this.draftNotes = this.client?.notes ?? '';
    this.notesSavedMessage = false;
  }

  saveNotes(): void {
    if (!this.client) {
      return;
    }

    this.notesSaved.emit({
      clientId: this.client.id,
      notes: this.draftNotes.trim()
    });
    this.notesSavedMessage = true;
  }

  get tradeValue(): number {
    return this.client?.trades.reduce((total, trade) => total + trade.value, 0) ?? 0;
  }

}