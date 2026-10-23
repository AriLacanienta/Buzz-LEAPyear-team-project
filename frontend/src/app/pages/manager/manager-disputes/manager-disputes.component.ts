import { Component, DestroyRef, Input, OnChanges, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Dialog, DialogModule } from '@angular/cdk/dialog';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, of, startWith, switchMap, tap } from 'rxjs';
import { Dispute, DisputePage, DisputeSearch } from '../../../models/dispute.model';
import { DisputeResolutionComponent } from '../../../components/manager/dispute-resolution/dispute-resolution.component';
import { DisputeService } from '../../../services/dispute.service';

@Component({
  selector: 'app-manager-disputes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DialogModule],
  templateUrl: './manager-disputes.component.html',
  styleUrls: ['./manager-disputes.component.scss']
})
export class ManagerDisputesComponent implements OnInit, OnChanges {
  @Input() userId?: number;
  readonly filters = new FormGroup({
    status: new FormControl<'OPEN' | 'RESOLVED' | ''>('OPEN', { nonNullable: true }),
    accountId: new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^[1-9]\d*$/)] }),
    fromDate: new FormControl('', { nonNullable: true }),
    toDate: new FormControl('', { nonNullable: true }),
    minTradeValue: new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^\d+(\.\d+)?$/)] }),
    maxTradeValue: new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^\d+(\.\d+)?$/)] }),
    sortBy: new FormControl<DisputeSearch['sortBy']>('createdAt', { nonNullable: true }),
    sortDirection: new FormControl<DisputeSearch['sortDirection']>('desc', { nonNullable: true }),
    size: new FormControl(20, { nonNullable: true, validators: [Validators.min(1), Validators.max(100)] })
  });
  result: DisputePage | null = null;
  readonly expandedDisputeIds = new Set<number>();
  loading = false;
  error = '';
  validationError = '';
  private query: DisputeSearch = { page: 0, size: 20, sortBy: 'createdAt', sortDirection: 'desc', status: 'OPEN' };
  private readonly requests = new Subject<void>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(Dialog);

  constructor(private readonly disputes: DisputeService) {}

  ngOnChanges(): void {
    this.resetSearch();
  }

  private resetSearch(): void {
    this.validationError = '';
    this.filters.reset();
    this.query = this.userId === undefined
      ? { page: 0, size: 20, sortBy: 'createdAt', sortDirection: 'desc', status: 'OPEN' }
      : { page: 0, size: 20, sortBy: 'status', sortDirection: 'asc', userId: this.userId };
    this.requests.next();
  }

  ngOnInit(): void {
    this.requests.pipe(
      startWith(undefined),
      switchMap(() => {
        this.loading = true;
        this.error = '';
        this.result = null;
        this.expandedDisputeIds.clear();
        return this.disputes.search({ ...this.query }).pipe(
          switchMap(page => {
            if (page.number > 0 && page.number >= page.totalPages) {
              this.query = { ...this.query, page: Math.max(0, page.totalPages - 1) };
              return this.disputes.search({ ...this.query });
            }
            return of(page);
          }),
          tap(page => {
            this.result = page;
            this.loading = false;
          }),
          catchError(() => {
            this.loading = false;
            this.error = 'Unable to load disputes. Please retry.';
            return EMPTY;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  applyFilters(): void {
    if (this.userId !== undefined) {
      this.resetSearch();
      return;
    }
    this.validationError = '';
    const values = this.filters.getRawValue();
    if (this.filters.invalid) {
      this.validationError = 'Enter a positive whole account ID, non-negative values, and a page size from 1 to 100.';
    } else if (values.fromDate && values.toDate && values.fromDate > values.toDate) {
      this.validationError = 'The start date must not be after the end date.';
    } else if (values.minTradeValue !== '' && values.maxTradeValue !== ''
      && Number(values.minTradeValue) > Number(values.maxTradeValue)) {
      this.validationError = 'The minimum trade value must not exceed the maximum.';
    }
    if (this.validationError) {
      this.filters.markAllAsTouched();
      return;
    }
    this.query = { ...values, page: 0 };
    this.requests.next();
  }

  clearFilters(): void {
    this.filters.reset();
    this.applyFilters();
  }

  changePage(offset: number): void {
    if (this.loading || !this.result) {
      return;
    }
    const page = this.result.number + offset;
    if (page >= 0 && page < this.result.totalPages) {
      this.query = { ...this.query, page };
      this.requests.next();
    }
  }

  retry(): void {
    this.requests.next();
  }

  openResolution(dispute: Dispute): void {
    if (dispute.status !== 'OPEN') {
      return;
    }
    this.dialog.open<boolean, number>(DisputeResolutionComponent, {
      data: dispute.id,
      width: '36rem',
      maxWidth: '95vw',
      ariaLabelledBy: 'dispute-resolution-title',
      autoFocus: 'textarea'
    }).closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(resolved => {
      if (resolved === true) {
        this.requests.next();
      }
    });
  }

  toggleDispute(id: number): void {
    if (this.expandedDisputeIds.has(id)) {
      this.expandedDisputeIds.delete(id);
    } else {
      this.expandedDisputeIds.add(id);
    }
  }

  trackById(_: number, dispute: { id: number }): number {
    return dispute.id;
  }
}
