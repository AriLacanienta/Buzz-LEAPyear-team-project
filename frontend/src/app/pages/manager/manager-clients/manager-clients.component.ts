import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';

import { ClientDetailComponent } from '../../../components/manager/clients/client-detail/client-detail.component';
import { ClientDirectoryComponent } from '../../../components/manager/clients/client-directory/client-directory.component';
import { ManagerClient } from '../../../models/manager-client.model';
import { ManagerTradeLogService } from '../../../services/manager-trade-log.service';
import { ManagerTradeLog } from '../../../models/manager-trade-log.model';
import { UserResponse, UserService } from '../../../services/user.service';

@Component({
  selector: 'app-manager-clients',
  standalone: true,
  imports: [ClientDirectoryComponent, ClientDetailComponent],
  templateUrl: './manager-clients.component.html',
  styleUrls: ['./manager-clients.component.scss']
})
export class ManagerClientsComponent implements OnInit {
  clients: ManagerClient[] = [];
  selectedClient: ManagerClient | null = null;
  searchTerm = '';
  loading = true;
  error = false;

  constructor(
    private readonly userService: UserService,
    private readonly tradeLogService: ManagerTradeLogService
  ) {}

  ngOnInit(): void {
    this.loadClients();
  }

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

  get selectedTrades(): ManagerTradeLog[] {
    return this.selectedClient?.trades ?? [];
  }

  get selectedTradeValue(): number {
    return this.selectedTrades.reduce(
      (total, trade) => total + trade.value,
      0
    );
  }

  loadClients(): void {
    this.loading = true;
    this.error = false;

    forkJoin({
      users: this.userService.getAllUsers(),
      trades: this.tradeLogService.getTradeLog(0, 100)
    }).subscribe({
      next: ({ users, trades }) => {
        this.clients = this.buildClients(users, trades.content);
        this.selectedClient = this.clients[0] ?? null;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  selectClient(client: ManagerClient): void {
    this.selectedClient = client;
  }

  trackByClientId(_index: number, client: ManagerClient): number {
    return client.id;
  }

  trackByTradeId(_index: number, trade: ManagerTradeLog): number {
    return trade.tradeId;
  }

  statusClass(status: string | null): string {
    return `status-${(status ?? 'unknown').toLowerCase()}`;
  }

  private buildClients(
    users: UserResponse[],
    trades: ManagerTradeLog[]
  ): ManagerClient[] {
    const clientUsers = users.filter(user =>
      user.roles.some(role => role.toUpperCase().includes('CLIENT'))
    );

    return clientUsers.map(user => {
      const name = `${user.firstName} ${user.lastName}`.trim();

      const userTrades = trades.filter(trade =>
        trade.clientEmail.toLowerCase() === user.email.toLowerCase() ||
        trade.clientName.toLowerCase() === name.toLowerCase()
      );

      const rejectedOrders = userTrades.filter(trade =>
        trade.status?.toUpperCase() === 'REJECTED'
      ).length;

      return {
        id: user.id,
        name,
        email: user.email,
        reference: `CLT-${String(user.id).padStart(4, '0')}`,
        initials: `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase(),
        trades: userTrades,
        rejectedOrders,
        totalValue: userTrades.reduce(
          (total, trade) => total + trade.value,
          0
        )
      };
    });
  }
}