import { Component, Input, OnDestroy, ChangeDetectorRef, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { HighchartsChartModule } from 'highcharts-angular';
import * as Highcharts from 'highcharts';

@Component({
  selector: 'app-portfolio-holding-card',
  templateUrl: './portfolio-holding-card.component.html',
  styleUrls: ['./portfolio-holding-card.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule, HighchartsChartModule]
})
export class PortfolioHoldingCardComponent implements OnDestroy, OnChanges {
  // Input properties - holding data and card position in grid
  @Input() holding: any;
  @Input() index: number = 0;
  Math = Math;

  // Highcharts configuration and options for the areaspline price chart
  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: Highcharts.Options = {};
  
  // Color palette for visual differentiation across multiple cards in portfolio
  private colors = ['#6366F1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];
  
  // Rolling window of price data points (max 8 points) for trend visualization
  private priceHistory: number[] = [];
  
  // Track current symbol to detect when holding changes and reinitialize chart data 

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['holding']) {
      const currentPrice = this.holding?.currentPrice || 0;
      const targetSymbol = this.holding?.instrumentSymbol || '';
      
      if (currentPrice > 0 && targetSymbol) {
        const parsedPrice = Number(currentPrice.toFixed(2));

        // Check if price history is empty or holding symbol changed (new stock)
        if (this.priceHistory.length === 0 || this.currentSymbol !== targetSymbol) {
          this.currentSymbol = targetSymbol;
          
          // Initialize with 6 simulated price points using slight variations from current price
          // Variation factor scaled by card index to ensure visual differentiation
          const randomFactor = 0.004 * (this.index + 1); 
          const baseVariations = [-4, -2, -3.5, 1, -1.5, 0];
          
          this.priceHistory = baseVariations.map(variant => {
            const simulatedValue = parsedPrice * (1 + (variant * randomFactor));
            return Number(simulatedValue.toFixed(2));
          });
          
          // Force the last point to actual current price for accuracy
          this.priceHistory[this.priceHistory.length - 1] = parsedPrice;
          this.generateChartOptions();
        } else {
          // Real-time price update - only update chart if price has changed
          const lastSavedPrice = this.priceHistory[this.priceHistory.length - 1];
          if (parsedPrice !== lastSavedPrice) {
            this.priceHistory.push(parsedPrice);
            
            // Maintain rolling 8-point window for performance and visual clarity
            if (this.priceHistory.length > 8) {
              this.priceHistory.shift();
            }
            
            this.generateChartOptions();
          }
        }
        
        this.cdr.detectChanges();
      }
    }
  }

  generateChartOptions(): void {
    // Generate timestamps assuming 5-minute intervals between data points
    const times = this.priceHistory.map((_, i) => {
      const now = new Date();
      const time = new Date(now.getTime() - (this.priceHistory.length - i - 1) * 5 * 60000);
      return time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    });

    // Calculate dynamic Y-axis range based on price history
    const minPrice = Math.min(...this.priceHistory);
    const maxPrice = Math.max(...this.priceHistory);
    const priceRange = maxPrice - minPrice;

    // Add 10% padding buffer to prevent data points from touching chart edges
    const paddingBuffer = priceRange > 0 ? priceRange * 0.10 : 1.0;

    this.chartOptions = {
      chart: {
        type: 'areaspline',
        backgroundColor: 'transparent',
        height: 140,
        margin: [15, 0, 15, 0], // Top, Right, Bottom, Left - minimal horizontal margins
        spacing: [0, 0, 0, 0]   // No spacing between chart and container
      },
      title: { text: '' },
      xAxis: {
        categories: times,
        visible: false,
        minPadding: 0,
        maxPadding: 0,
        startOnTick: false,
        endOnTick: false
      },
      yAxis: {
        title: { text: '' },
        labels: { enabled: false },
        gridLineWidth: 0,
        lineWidth: 0,
        tickWidth: 0,
        startOnTick: false, 
        endOnTick: false,
        min: minPrice - paddingBuffer, // Dynamic floor with padding
        max: maxPrice + paddingBuffer  // Dynamic ceiling with padding
      },
      tooltip: {
        pointFormat: '<b>\${point.y:.2f}</b>',
        headerFormat: '',
        backgroundColor: 'rgba(15, 21, 32, 0.95)',
        borderColor: '#1E2D45',
        style: { color: '#FFFFFF' }
      },
      legend: { enabled: false },
      plotOptions: {
        areaspline: {
          dataLabels: { 
            enabled: true,
            format: '\${point.y:.2f}',
            style: {
              fontSize: '10px',
              fontWeight: '600',
              color: '#62748E',
              textOutline: 'none'
            },
            y: -6  // Position label above the data point
          },
          enableMouseTracking: true,
          animation: false  // Disabled for real-time update performance
        }
      },
      series: [{
        type: 'areaspline',
        name: this.holding?.instrumentSymbol || 'Price',
        data: [...this.priceHistory],
        // Select color from palette based on card position for visual differentiation
        color: this.colors[this.index % this.colors.length],
        lineWidth: 2,
        marker: { 
          enabled: true, 
          radius: 3, 
          fillColor: this.colors[this.index % this.colors.length] 
        },
        // Gradient fill creates visual depth - opaque at top, transparent at bottom
        fillColor: {
          linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
          stops: [
            [0, this.colors[this.index % this.colors.length] + '26'],  // 15% opacity at top
            [1, this.colors[this.index % this.colors.length] + '00']   // Transparent at bottom
          ]
        }
      }],
      credits: { enabled: false }
    };
  }

  formatPrice(price: number): string {
    if (!price) return '0.00';
    return price.toFixed(2);
  }

  formatTotalValue(value: number): string {
    if (!value) return '0.00';
    return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  getChangePercentClass(value: number): string {
    if (value > 0) return 'positive-change';
    if (value < 0) return 'negative-change';
    return 'neutral-change';
  }

  getChangeArrow(value: number): string {
    if (value > 0) return '↗';
    if (value < 0) return '↘';
    return '';
  }

  getSymbolFirstLetter(symbol: string): string {
    return symbol?.charAt(0).toUpperCase() || 'S';
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
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})\$/i.exec(hex);
    return result ? `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}` : '99, 102, 241';
  }

  ngOnDestroy(): void {}
}
