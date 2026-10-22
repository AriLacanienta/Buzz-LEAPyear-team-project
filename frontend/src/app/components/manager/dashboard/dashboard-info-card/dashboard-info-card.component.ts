import { Component, Input } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
	selector: 'app-dashboard-info-card',
	standalone: true,
	imports: [MatCardModule, NgFor, NgIf],
	templateUrl: './dashboard-info-card.component.html',
	styleUrls: ['./dashboard-info-card.component.scss']
})
export class DashboardInfoCardComponent {
	@Input() header: string = '';
	@Input() data: string = '';
	@Input() info: string = '';
	@Input() dataLines: string[] = [];

	getFillRateColor(): string {
		if (this.data.includes('%')) {
			const value = parseFloat(this.data);
			if (value >= 90) return '#10b981';
			if (value >= 75) return '#f59e0b';
			return '#ef4444';
		}
		return 'inherit';
	}

	getOpenDisputesColor(): string {
		const value = parseInt(this.data.replace(/,/g, ''), 10);
		if (!isNaN(value)) {
			if (value === 0) return '#10b981';
			if (value <= 3) return '#f59e0b';
			return '#ef4444';
		}
		return 'inherit';
	}
}
