import { Component, Input, OnDestroy, ChangeDetectorRef, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { HighchartsChartModule } from 'highcharts-angular';
import * as Highcharts from 'highcharts';
import { ChartHistoryService, ChartSample } from '../../../../services/chart-history.service';

@Component({
  selector: 'app-portfolio-holding-card',
  templateUrl: './portfolio-holding-card.component.html',
  styleUrls: ['./portfolio-holding-card.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule, HighchartsChartModule]
})
export class PortfolioHoldingCardComponent implements OnDestroy, OnChanges {
  @Input() holding: any;
  @Input() index: number = 0;
  @Input() accountId: number | null = null;
  Math = Math;

  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: Highcharts.Options = {};
  updateFlag = false;
  
  // Color palette for visual differentiation across multiple cards in portfolio
  private colors = ['#6366F1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];
  
  // Rolling window of price data points (max 8 points) for trend visualization
  private priceHistory: ChartSample[] = [];
  
  // Track current symbol to detect when holding changes and reinitialize chart data 
  private currentSymbol: string = '';
  

  constructor(private cdr: ChangeDetectorRef, private chartHistory: ChartHistoryService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['holding'] || changes['accountId']) {
      const currentValue = Number(this.holding?.totalValue);
      const targetSymbol = this.holding?.instrumentSymbol || '';

      if (Number.isFinite(currentValue) && currentValue >= 0 && targetSymbol && this.accountId !== null) {
        const parsedValue = Number(currentValue.toFixed(2));

        this.priceHistory = this.chartHistory.read(this.accountId, `holding-value:${targetSymbol}`);
        if (!this.priceHistory.length) {
          this.priceHistory = this.chartHistory.record(this.accountId, `holding-value:${targetSymbol}`, parsedValue);
        }
        this.generateChartOptions();

        this.cdr.detectChanges();
      }
    }
  }

  generateChartOptions(): void {
    const values = this.priceHistory.map(([, value]) => value);

    const minPrice = Math.min(...values);
    const maxPrice = Math.max(...values);
    const priceRange = maxPrice - minPrice;

    const paddingBuffer = priceRange > 0 ? priceRange * 0.10 : 1.0;

    this.chartOptions = {
      chart: {
        type: 'areaspline',
        backgroundColor: 'transparent',
        height: 140,
        margin: [15, 0, 15, 0],
        spacing: [0, 0, 0, 0]
      },
      title: { text: '' },
      time: { timezone: Intl.DateTimeFormat().resolvedOptions().timeZone },
      xAxis: {
        type: 'datetime',
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
        min: minPrice - paddingBuffer,
        max: maxPrice + paddingBuffer
      },
      tooltip: {
        pointFormat: '<b>\${point.y:.2f}</b>',
        xDateFormat: '%b %e, %Y %H:%M:%S',
        backgroundColor: 'rgba(15, 21, 32, 0.95)',
        borderColor: '#1E2D45',
        style: { color: '#FFFFFF' }
      },
      legend: { enabled: false },
      plotOptions: {
        areaspline: {
          dataLabels: {
            enabled: false,
            format: '\${point.y:.2f}',
            style: {
              fontSize: '10px',
              fontWeight: '600',
              color: '#62748E',
              textOutline: 'none'
            },
            y: -6
          },
          enableMouseTracking: true,
          animation: false
        }
      },
      series: [{
        type: 'areaspline',
        name: this.holding?.instrumentSymbol || 'Price',
        data: [...this.priceHistory],
        color: this.colors[this.index % this.colors.length],
        lineWidth: 2,
        marker: {
          enabled: true,
          radius: 3,
          fillColor: this.colors[this.index % this.colors.length]
        },
        fillColor: {
          linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
          stops: [
            [0, this.colors[this.index % this.colors.length] + '26'],
            [1, this.colors[this.index % this.colors.length] + '00']
          ]
        }
      }],
      credits: { enabled: false }
    };

    this.updateFlag = !this.updateFlag;
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
