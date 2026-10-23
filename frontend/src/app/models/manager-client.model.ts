import { ManagerTradeLog } from './manager-trade-log.model';

export interface ManagerClient {
	id: number;
	name: string;
	email: string;
	reference: string;
	initials: string;
	trades: ManagerTradeLog[];
	rejectedOrders: number;
	orderTotal: number;
	totalValue: number;
	notes?: string;
}
