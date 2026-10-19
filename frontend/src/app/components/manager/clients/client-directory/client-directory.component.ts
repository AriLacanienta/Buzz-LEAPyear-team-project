import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ManagerClient } from '../../../../models/manager-client.model';

@Component({
  selector: 'app-client-directory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './client-directory.component.html',
  styleUrls: ['./client-directory.component.scss']
})
export class ClientDirectoryComponent {
  @Input() clients: ManagerClient[] = [];
  @Input() selectedClient: ManagerClient | null = null;
  @Input() loading = false;
  @Input() error = false;
  @Output() clientSelected = new EventEmitter<ManagerClient>();
  @Output() retryRequested = new EventEmitter<void>();

  searchTerm = '';

  get filteredClients(): ManagerClient[] {
    const search = this.searchTerm.trim().toLowerCase();

    if (!search) {
      return this.clients;
    }

    return this.clients.filter(client =>
      client.name.toLowerCase().includes(search) ||
      client.email.toLowerCase().includes(search) ||
      client.reference.toLowerCase().includes(search)
    );
  }

  trackByClientId(_index: number, client: ManagerClient): number {
    return client.id;
  }
}