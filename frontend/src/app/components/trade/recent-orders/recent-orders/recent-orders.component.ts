import { Component } from '@angular/core';
import { NgIf, NgFor, DatePipe } from '@angular/common';

export interface Order {
  id: string;
  symbol: string;
  quantity: number;
  price: number;
  type: 'BUY' | 'SELL';
  timestamp: Date;
}

@Component({
  selector: 'app-recent-orders',
  standalone: true,
  imports: [NgIf, NgFor,DatePipe],
  templateUrl: './recent-orders.component.html',
  styleUrl: './recent-orders.component.scss'
})
export class RecentOrdersComponent {
  orders: Order[] = [];

  constructor() {}
  
}
