import { Component, Input, OnInit, OnDestroy, HostListener, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortfolioHoldingCardComponent } from '@app/components/portfolio/portfolio-holding-card/portfolio-holding-card.component';

@Component({
  selector: 'app-portfolio-holdings-container',
  templateUrl: './portfolio-holdings-container.component.html',
  styleUrls: ['./portfolio-holdings-container.component.scss'],
  imports: [CommonModule, PortfolioHoldingCardComponent],
  standalone: true
})
export class PortfolioHoldingsContainerComponent implements OnInit, OnDestroy, OnChanges {
  @Input() title: string = '';
  @Input() holdings: any[] = [];
  @Input() isLoading: boolean = false;
  
  visibleHoldings: any[] = [];

  ngOnInit() {
    this.calculateVisibleHoldings();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['holdings']) {
      this.calculateVisibleHoldings();
    }
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.calculateVisibleHoldings();
  }

  ngOnDestroy() {}

  private calculateVisibleHoldings() {
    const availableWidth = window.innerWidth * 0.85;
    const cardWidth = 216;
    const gap = 8;
    const cardWithGap = cardWidth + gap;
    
    const maxCards = Math.floor(availableWidth / cardWithGap);
    
    const cappedMaxCards = Math.min(maxCards, 6);
    const visibleCount = Math.max(cappedMaxCards, 1);
    
    this.visibleHoldings = this.holdings.slice(0, visibleCount);
  }
}
