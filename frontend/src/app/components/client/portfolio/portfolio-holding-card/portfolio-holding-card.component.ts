import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-portfolio-holding-card',
  templateUrl: './portfolio-holding-card.component.html',
  styleUrls: ['./portfolio-holding-card.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule]
})
export class PortfolioHoldingCardComponent {
  @Input() holding: any;
  @Input() index: number = 0;
  Math = Math;
  
  private colors = ['#6366F1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

  formatPrice(price: number): string {
    if (!price) return '0.00';
    return price.toFixed(2);
  }

  formatTotalValue(value: number): string {
    if (!value) return '0.00';
    return value.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  getChangePercentClass(value: number): string {
    if (value > 0) {
      return 'positive-change';
    } else if (value < 0) {
      return 'negative-change';
    } else {
      return 'neutral-change';
    }
  }

  getChangeArrow(value: number): string {
    if (value > 0) {
      return '↗';
    } else if (value < 0) {
      return '↘';
    } else {
      return '';
    }
  }

  getSymbolFirstLetter(symbol: string): string {
    return symbol.charAt(0).toUpperCase();
  }

  getSymbolColor(symbol: string): string {
    return this.colors[this.index % this.colors.length];
  }

  getSymbolStyle(symbol: string): any {
    const color = this.getSymbolColor(symbol);
    const rgb = this.hexToRgb(color);
    return {
      '--symbol-color': color,
      '--symbol-bg': `rgba(${rgb}, 0.2)`
    };
  }

  hexToRgb(hex: string): string {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}` : '99, 102, 241';
  }
}