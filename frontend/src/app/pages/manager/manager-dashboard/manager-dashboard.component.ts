import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe, NgIf } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, exhaustMap, finalize, merge, tap, timer } from 'rxjs';
import { DashboardInfoCardComponent } from '@app/components/manager/dashboard/dashboard-info-card/dashboard-info-card.component';
import { ManagerDashboardSummary } from '../../../models/manager-dashboard.model';
import { ManagerDashboardService } from '../../../services/manager-dashboard.service';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [DashboardInfoCardComponent, DatePipe, DecimalPipe, NgIf],
  templateUrl: './manager-dashboard.component.html',
  styleUrls: ['./manager-dashboard.component.scss']
})
export class ManagerDashboardComponent implements OnInit {
  summary: ManagerDashboardSummary | null = null;
  loading = true;
  error = false;
  private readonly destroyRef = inject(DestroyRef);
  private readonly retries = new Subject<void>();

  constructor(private readonly dashboardService: ManagerDashboardService) {}

  ngOnInit(): void {
    merge(timer(0, 30000), this.retries).pipe(
      exhaustMap(() => {
        this.loading = true;
        return this.dashboardService.getSummary().pipe(
          tap(summary => {
            this.summary = summary;
            this.error = false;
          }),
          catchError(() => {
            this.error = true;
            return EMPTY;
          }),
          finalize(() => this.loading = false)
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  retry(): void {
    this.retries.next();
  }

  get ordersComparison(): string {
    if (!this.summary) {
      return '';
    }
    const difference = new Intl.NumberFormat('en-US', { signDisplay: 'exceptZero' })
      .format(this.summary.ordersDifference);
    const percentage = this.summary.ordersChangePercent === null ? 'N/A'
      : `${new Intl.NumberFormat('en-US', {
          signDisplay: 'exceptZero', minimumFractionDigits: 1, maximumFractionDigits: 1
        }).format(this.summary.ordersChangePercent)}%`;
    return `${difference} (${percentage}) vs yesterday`;
  }

  get currencyTotals(): string[] {
    return this.summary?.assetsMovedByCurrency.map(total =>
      new Intl.NumberFormat('en-US', {
        style: 'currency', currency: total.currencyCode, currencyDisplay: 'code'
      }).format(total.totalValue)
    ) ?? [];
  }
}
