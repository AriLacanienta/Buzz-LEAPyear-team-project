import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortfolioHoldingCardComponent } from '@app/components/portfolio/portfolio-holding-card/portfolio-holding-card.component';

@Component({
  selector: 'app-portfolio-holdings-container',
  templateUrl: './portfolio-holdings-container.component.html',
  styleUrls: ['./portfolio-holdings-container.component.scss'],
  imports: [CommonModule, PortfolioHoldingCardComponent],
  standalone: true
})
export class PortfolioHoldingsContainerComponent {
  @Input() holdings: any[] = [];
  @Input() isLoading: boolean = false;
}
