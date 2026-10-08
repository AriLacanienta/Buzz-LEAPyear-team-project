  import { Component, DestroyRef, OnInit } from '@angular/core';
import { HoldingService } from '@app/services/holding.service';
import { TradeOrderService } from '@app/services/trade-order.service';
import { TradeOrderPreviewRequest, TradeOrderPreviewResponse } from '@app/models/trade-order.model';
import { SearchComponent } from '../search/search.component';
import { SearchResultDetailComponent } from '../search-result-detail/search-result-detail.component';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';
import { AccountService } from '@app/services/account.service';
import { merge, switchMap, timer, of, Subject } from 'rxjs';
import { catchError, debounceTime, filter, finalize, take, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

type OrderType = 'BUY' | 'SELL';

const PREVIEW_POLL_INTERVAL_MS = 3000;
const SUCCESS_MESSAGE_TIMEOUT = 2000;

@Component({
  selector: 'app-buy-sell',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, SearchComponent, SearchResultDetailComponent],
  templateUrl: './buy-sell.component.html',
  styleUrl: './buy-sell.component.scss'
})
export class BuySellComponent implements OnInit {
  isBuy: boolean = true;
  quantity: number = 0;
  successMessage: string = '';
  errorMessage: string = '';
  selectedTag: string = 'All';
  selectedResult: InstrumentSearchResponse | null = null;
  instruments: InstrumentSearchResponse[] | null = null;
  preview: TradeOrderPreviewResponse | null = null;
  cashAvailable: number | null = null;
  isSubmitting: boolean = false;

  private accountId: string | null = null;
  private ownsSelected: boolean | null = null;
  private previewTrigger$ = new Subject<void>();

  constructor(
    private accountService: AccountService, 
    private holdingService: HoldingService,
    private tradeOrderService: TradeOrderService, 
    private destroyRef: DestroyRef
  ) {}

  ngOnInit(): void {
    this.accountService.getMyAccount().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: account => {
        this.accountId = account.accountId;
        this.loadCashAvailable();
        this.checkOwnership();
        this.requestPreview();
      }
    })

    merge(
      this.previewTrigger$.pipe(debounceTime(300)),
      timer(0, PREVIEW_POLL_INTERVAL_MS)
    ).pipe(
      switchMap(() => {
        const request = this.buildPreviewRequest();
        if (!request) {
          return of(null);
        }
        return this.tradeOrderService.previewOrder(request).pipe(catchError(() => of(null)));
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(preview => {
      this.preview = preview;
      if (preview) {
        this.cashAvailable = preview.cashAvailable;
      }
    });
  }

  toggleOrderType(side: OrderType): void {
    this.isBuy = side === 'BUY';
    this.requestPreview();
  }

  selectTag(tag: string): void {
    this.selectedTag = tag;
  }

  onQuantityInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    // Remove non-numeric characters
    input.value = input.value.replace(/[^0-9]/g, '');
    // Remove leading zeros (but keep at least one digit)
    input.value = input.value.replace(/^0+(?=\d)/, '');

    this.quantity = parseInt(input.value, 10) || 0;
    // Clear error and success messages when user changes quantity
    this.errorMessage = '';
    this.successMessage = '';
    this.requestPreview();
  }

  onSearchItemSelected(item: InstrumentSearchResponse | null): void {
    const symbolChanged = item?.instrumentSymbol !== this.selectedResult?.instrumentSymbol;
    this.selectedResult = item;

    if (symbolChanged || item === null) {
      this.successMessage = '';
      this.errorMessage = '';
      this.checkOwnership();
    }
    this.requestPreview();
  }

  onPlaceOrder(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.selectedResult) {
      this.errorMessage = 'Please select an instrument.';
      return;
    }

    if (!Number.isInteger(this.quantity) || this.quantity <= 0) {
      this.errorMessage = 'Please enter a valid quantity greater than 0.';
      return;
    }

    const side = this.isBuy ? 'BUY' : 'SELL';
    if (this.isPreviewRejected(side)) {
      return;
    }
    this.submitOrder(side);
  }

  submitOrder(side: 'BUY' | 'SELL'): void {
    if (!this.selectedResult || !this.accountId) {
      this.errorMessage = 'Account not loaded yet. Please try again.';
      return;
    }
    const symbol = this.selectedResult?.instrumentSymbol;
    const quantity = this.quantity;
    const price = this.preview?.livePrice ?? this.selectedResult.currentPrice;

    this.isSubmitting = true;
    this.tradeOrderService.submitOrder({
      side,
      instrumentSymbol: symbol,
      quantity,
      price,
      accountId: this.accountId
    }).pipe(
      finalize(() => this.isSubmitting = false),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: response => {
        this.quantity = 0;
        this.successMessage = `Order #${response.orderId} submitted`;
        this.tradeOrderService.notifyOrderplaced();
        this.watchOrderCompletion(response.orderId, side, quantity, symbol);
      },
      error: () => this.errorMessage = 'Order could not be submitted'
    });
  }

  private watchOrderCompletion(orderId: number, side: 'BUY' | 'SELL', quantity: number, symbol: string): void {
    timer(0, 300).pipe(
      switchMap(() => this.tradeOrderService.getOrderStatus(orderId).pipe(catchError(() => of(null)))),
      tap(status => {
        if (status?.status === 'ACCEPTED') {
          this.successMessage = `Order #${orderId} accepted`;
        }
      }),
      filter(status => status !== null && (status.status === 'FILLED' || status.status === 'REJECTED')),
      take(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(status => {
      if (!status) {
        return;
      }

      this.tradeOrderService.notifyOrderCompleted(status.status);
      this.loadCashAvailable();
      this.checkOwnership();
      this.requestPreview();

      if (status.status === 'FILLED') {
        this.successMessage = `${side === 'BUY' ? 'Bought' : 'Sold'} ${quantity} ${symbol} (order #${orderId})`;
        setTimeout(() => {
          this.successMessage = '';
        }, SUCCESS_MESSAGE_TIMEOUT);
      }
      else {
        // Clear the submitted message and show the error
        this.successMessage = '';
        this.errorMessage = this.formatErrorMessage(status.reasonText) || 'Order was rejected.';
      }
    });
  }

  private checkOwnership(): void {
    this.ownsSelected = null;
    const symbol = this.selectedResult?.instrumentSymbol;
    if (!this.accountId || !symbol) {
      return;
    }
    this.holdingService.getHoldingsByAccountId(this.accountId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: holdings => {
        if (symbol === this.selectedResult?.instrumentSymbol) {
          this.ownsSelected = holdings.some(h => h.instrumentSymbol === symbol);
        }
      },
      error: error => console.error('Could not load holdings', error)
    });
  }

  private isPreviewRejected(side: 'BUY' | 'SELL'): boolean {
    const preview = this.preview;
    const matchesInput = preview
      && preview.side === side
      && preview.symbol === this.selectedResult?.instrumentSymbol
      && Number(preview.quantity) === this.quantity;

    if (preview && matchesInput && !preview.valid) {
      this.errorMessage = preview.reason ?? 'Order is not valid.';
      return true;
    }
    return false;
  }

  private requestPreview(): void {
    this.previewTrigger$.next();
  }

  private buildPreviewRequest(): TradeOrderPreviewRequest | null {
    if (!this.accountId || !this.selectedResult || this.quantity <= 0) {
      return null;
    }

    return {
      side: this.isBuy ? 'BUY' : 'SELL',
      instrumentSymbol: this.selectedResult.instrumentSymbol,
      quantity: this.quantity,
      accountId: this.accountId
    };
  }

  private loadCashAvailable(): void {
    if (!this.accountId) {
      return;
    }
    this.accountService.getAccountBalance(this.accountId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: balance => this.cashAvailable = balance.cashAvailable,
      error: error => console.error('Could not load account balance', error)
    });
  }

  get estimatedTotal(): number | null {
    if (this.preview?.estimatedValue != null) {
      return this.preview.estimatedValue;
    }
    return this.selectedResult && this.quantity > 0 ? this.selectedResult.currentPrice * this.quantity : null;
  }

  get indicativePrice(): number | null {
    return this.preview?.livePrice ?? this.selectedResult?.currentPrice ?? null;
  }

  get buyingPowerValue(): number | null {
    return this.preview?.cashAfter ?? this.cashAvailable;
  }

  get isNotHeld(): boolean {
    return !this.isBuy && this.ownsSelected === false;
  }

  get isInsufficientCash(): boolean {
    return this.isBuy && this.buyingPowerValue != null && this.buyingPowerValue < 0;
  }

  get buyingPowerLabel(): string {
    if (!this.preview) {
      return 'Available buying power';
    }
    return this.isBuy ? 'Available buying power' : 'Cash after sale';
  }

  private formatErrorMessage(message: string | null): string | null {
    if (!message) return null;
    return message.replace(/\b(\d+)\.0+\b/g, '$1');
  }
}
