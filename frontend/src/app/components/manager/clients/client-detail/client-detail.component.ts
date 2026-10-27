import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ManagerClient } from '../../../../models/manager-client.model';
import { AdminSessionService } from '../../../../services/admin-session.service';
import { ClientTradeHistoryComponent } from '../client-trade-history/client-trade-history.component';
import { ManagerDisputesComponent } from '@app/pages/manager/manager-disputes/manager-disputes.component';

type SessionStatus = 'loading' | 'active' | 'inactive' | 'error';

@Component({
  selector: 'app-client-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ClientTradeHistoryComponent, ManagerDisputesComponent],
  templateUrl: './client-detail.component.html',
  styleUrls: ['./client-detail.component.scss']
})
export class ClientDetailComponent implements OnChanges, OnDestroy {
  @Input() client: ManagerClient | null = null;
  @Output() refreshRequested = new EventEmitter<void>();
  @Output() notesSaved = new EventEmitter<{ clientId: number; notes: string }>();

  draftNotes = '';
  notesSavedMessage = false;

  sessionStatus: SessionStatus = 'loading';
  confirmingRevoke = false;
  revoking = false;
  revokeMessage = '';
  revokeError = '';
  private sessionRequest?: Subscription;

  constructor(private readonly adminSessionService: AdminSessionService) {}

  ngOnChanges(): void {
    this.draftNotes = this.client?.notes ?? '';
    this.notesSavedMessage = false;
    this.confirmingRevoke = false;
    this.revokeMessage = '';
    this.revokeError = '';
    this.loadSessionStatus();
  }

  ngOnDestroy(): void {
    this.sessionRequest?.unsubscribe();
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

  loadSessionStatus(): void {
    this.sessionRequest?.unsubscribe();

    if (!this.client) {
      return;
    }

    this.sessionStatus = 'loading';
    this.sessionRequest = this.adminSessionService.getActiveSessions(this.client.id).subscribe({
      next: sessions => {
        this.sessionStatus = sessions.length > 0 ? 'active' : 'inactive';
      },
      error: () => {
        this.sessionStatus = 'error';
      }
    });
  }

  revokeAccess(): void {
    if (!this.client) {
      return;
    }

    const clientId = this.client.id;
    this.revoking = true;
    this.revokeError = '';

    this.adminSessionService.revokeUserTokens(clientId).subscribe({
      next: () => {
        this.revoking = false;
        this.confirmingRevoke = false;

        if (this.client?.id === clientId) {
          this.sessionStatus = 'inactive';
          this.revokeMessage = 'Access and refresh tokens revoked';
        }
      },
      error: () => {
        this.revoking = false;
        this.revokeError = 'Unable to revoke tokens. Please try again.';
      }
    });
  }

  get tradeValue(): number {
    return this.client?.trades.reduce((total, trade) => total + trade.value, 0) ?? 0;
  }

}
