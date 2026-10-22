import {
  Component,
  DestroyRef,
  HostListener,
  OnInit,
  ViewChild,
} from "@angular/core";
import { HoldingService } from "@app/services/holding.service";
import { TradeOrderService } from "@app/services/trade-order.service";
import {
  QuantityMode,
  TradeOrderPreviewRequest,
  TradeOrderPreviewResponse,
} from "@app/models/trade-order.model";
import { SearchComponent } from "../search/search.component";
import { SearchResultDetailComponent } from "../search-result-detail/search-result-detail.component";
import { CommonModule, CurrencyPipe } from "@angular/common";
import { InstrumentSearchResponse } from "@app/models/instrument-search-response.model";
import { AccountService } from "@app/services/account.service";
import { merge, switchMap, timer, of, Subject, Subscription } from "rxjs";
import {
  catchError,
  debounceTime,
  filter,
  finalize,
  take,
  tap,
} from "rxjs/operators";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

type OrderType = "BUY" | "SELL";
type AssetTag = "Stock" | "Crypto" | "Foreign";

const TAG_ASSET_TYPES: Record<AssetTag, string[]> = {
  Stock: ["EQUITY", "ETF", "BOND"],
  Crypto: ["CRYPTO"],
  Foreign: ["EQUITY", "ETF", "BOND"],
};

const PREVIEW_POLL_INTERVAL_MS = 3000;
const SUCCESS_MESSAGE_TIMEOUT = 2000;
const ERROR_MESSAGE_TIMEOUT = 5000;

@Component({
  selector: "app-buy-sell",
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    SearchComponent,
    SearchResultDetailComponent,
  ],
  templateUrl: "./buy-sell.component.html",
  styleUrl: "./buy-sell.component.scss",
})
export class BuySellComponent implements OnInit {
  isBuy: boolean = true;
  quantity: number = 0;
  amount: number = 0;
  private amountInputValid: boolean = false;
  quantityMode: QuantityMode = "SHARES";
  successMessage: string = "";
  errorMessage: string = "";
  selectedTag: AssetTag = "Stock";
  selectedResult: InstrumentSearchResponse | null = null;
  instruments: InstrumentSearchResponse[] | null = null;
  preview: TradeOrderPreviewResponse | null = null;
  cashAvailable: number | null = null;
  isSubmitting: boolean = false;
  isConfirmationOpen: boolean = false;

  private accountId: string | null = null;
  private ownsSelected: boolean | null = null;
  private previewTrigger$ = new Subject<void>();
  private errorDismissal?: Subscription;

  @ViewChild(SearchComponent) private searchComponent?: SearchComponent;

  constructor(
    private accountService: AccountService,
    private holdingService: HoldingService,
    private tradeOrderService: TradeOrderService,
    private destroyRef: DestroyRef,
  ) {}

  ngOnInit(): void {
    this.accountService
      .getMyAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (account) => {
          this.accountId = account.accountId;
          this.loadCashAvailable();
          this.checkOwnership();
          this.requestPreview();
        },
      });

    merge(
      this.previewTrigger$.pipe(debounceTime(300)),
      timer(0, PREVIEW_POLL_INTERVAL_MS),
    )
      .pipe(
        switchMap(() => {
          const request = this.buildPreviewRequest();
          if (!request) {
            return of(null);
          }
          return this.tradeOrderService
            .previewOrder(request)
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((preview) => {
        this.preview = preview;
        if (preview) {
          this.cashAvailable = preview.cashAvailable;
        }
      });
  }

  toggleOrderType(side: OrderType): void {
    this.clearErrorMessage();
    this.isBuy = side === "BUY";
    this.requestPreview();
  }

  selectTag(tag: AssetTag): void {
    if (tag === this.selectedTag) {
      return;
    }
    this.selectedTag = tag;
    this.clearErrorMessage();
    this.searchComponent?.onSearchClear();
    this.applyQuantityModeConstraints(tag === "Crypto" ? "AMOUNT" : "SHARES");
  }

  setQuantityMode(mode: QuantityMode): void {
    if (
      (mode === "SHARES" && !this.canTradeWholeShares) ||
      (mode === "AMOUNT" && !this.canTradeByAmount)
    ) {
      return;
    }
    this.applyQuantityModeConstraints(mode);
  }

  onQuantityModeChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value === "SHARES" || value === "AMOUNT") {
      this.setQuantityMode(value);
    }
  }

  onAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const [whole, ...fraction] = input.value.replace(/[^0-9.]/g, "").split(".");
    let value = fraction.length
      ? `${whole}.${fraction.join("").slice(0, 2)}`
      : whole;
    value = value.replace(/^0+(?=\d)/, "");
    input.value = value;

    this.amount = parseFloat(input.value) || 0;
    this.amountInputValid =
      /^\d+(?:\.\d{1,2})?$/.test(value) && this.amount > 0;
    this.clearErrorMessage();
    this.successMessage = "";
    this.requestPreview();
  }

  onQuantityInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    // Remove non-numeric characters
    input.value = input.value.replace(/[^0-9]/g, "");
    // Remove leading zeros (but keep at least one digit)
    input.value = input.value.replace(/^0+(?=\d)/, "");

    this.quantity = parseInt(input.value, 10) || 0;
    // Clear error and success messages when user changes quantity
    this.clearErrorMessage();
    this.successMessage = "";
    this.requestPreview();
  }

  onSearchItemSelected(item: InstrumentSearchResponse | null): void {
    const symbolChanged =
      item?.instrumentSymbol !== this.selectedResult?.instrumentSymbol;
    this.selectedResult = item;

    if (symbolChanged || item === null) {
      this.successMessage = "";
      this.clearErrorMessage();
      this.checkOwnership();
      this.applyQuantityModeConstraints(this.quantityMode);
    }
    this.requestPreview();
  }

  onPlaceOrder(): void {
    this.clearErrorMessage();
    this.successMessage = "";

    if (!this.selectedResult) {
      this.errorMessage = "Please select an instrument.";
      return;
    }

    if (this.quantityMode === "AMOUNT") {
      if (!this.amountInputValid || !(this.amount > 0)) {
        this.errorMessage = "Please enter a valid amount greater than 0.";
        return;
      }
    } else if (!Number.isInteger(this.quantity) || this.quantity <= 0) {
      this.errorMessage = "Please enter a valid quantity greater than 0.";
      return;
    }

    const side = this.isBuy ? "BUY" : "SELL";
    if (this.isPreviewRejected(side)) {
      return;
    }
    this.isConfirmationOpen = true;
  }

  confirmOrder(): void {
    if (this.isSubmitting) {
      return;
    }

    const side = this.isBuy ? "BUY" : "SELL";
    this.isConfirmationOpen = false;
    this.submitOrder(side, this.indicativePrice);
  }

  cancelConfirmation(): void {
    this.isConfirmationOpen = false;
  }

  @HostListener("document:keydown.escape")
  closeConfirmationWithEscape(): void {
    if (this.isConfirmationOpen && !this.isSubmitting) {
      this.cancelConfirmation();
    }
  }

  submitOrder(
    side: "BUY" | "SELL",
    confirmedPrice: number | null = null,
  ): void {
    if (!this.selectedResult || !this.accountId) {
      this.errorMessage = "Account not loaded yet. Please try again.";
      return;
    }
    const symbol = this.selectedResult?.instrumentSymbol;
    const quantityMode = this.quantityMode;
    const quantity = this.quantity;
    const amount = this.amount;
    const price =
      confirmedPrice ??
      this.preview?.livePrice ??
      this.selectedResult.currentPrice;
    const orderLabel =
      quantityMode === "AMOUNT"
        ? `${this.formatUsd(amount)} of ${symbol}`
        : `${quantity} ${symbol}`;

    this.isSubmitting = true;
    this.tradeOrderService
      .submitOrder({
        side,
        instrumentSymbol: symbol,
        quantityMode,
        ...(quantityMode === "AMOUNT" ? { amount } : { quantity }),
        price,
        accountId: this.accountId,
      })
      .pipe(
        finalize(() => (this.isSubmitting = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.quantity = 0;
          this.amount = 0;
          this.successMessage = `Order #${response.orderId} submitted`;
          this.tradeOrderService.notifyOrderplaced();
          this.watchOrderCompletion(response.orderId, side, orderLabel);
        },
        error: () => this.showOrderError("Order could not be submitted"),
      });
  }

  private watchOrderCompletion(
    orderId: number,
    side: "BUY" | "SELL",
    orderLabel: string,
  ): void {
    timer(0, 300)
      .pipe(
        switchMap(() =>
          this.tradeOrderService
            .getOrderStatus(orderId)
            .pipe(catchError(() => of(null))),
        ),
        tap((status) => {
          if (status?.status === "ACCEPTED") {
            this.successMessage = `Order #${orderId} accepted`;
          }
        }),
        filter(
          (status) =>
            status !== null &&
            (status.status === "FILLED" || status.status === "REJECTED"),
        ),
        take(1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((status) => {
        if (!status) {
          return;
        }

        this.tradeOrderService.notifyOrderCompleted(status.status);
        this.loadCashAvailable();
        this.checkOwnership();
        this.requestPreview();

        if (status.status === "FILLED") {
          this.successMessage = `${side === "BUY" ? "Bought" : "Sold"} ${orderLabel} (order #${orderId})`;
          setTimeout(() => {
            this.successMessage = "";
          }, SUCCESS_MESSAGE_TIMEOUT);
        } else {
          // Clear the submitted message and show the error
          this.successMessage = "";
          this.showOrderError(
            this.formatErrorMessage(status.reasonText) || "Order was rejected.",
          );
        }
      });
  }

  private clearErrorMessage(): void {
    this.errorDismissal?.unsubscribe();
    this.errorMessage = "";
  }

  private showOrderError(message: string): void {
    this.clearErrorMessage();
    this.errorMessage = message;
    this.errorDismissal = timer(ERROR_MESSAGE_TIMEOUT)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => (this.errorMessage = ""));
  }

  private checkOwnership(): void {
    this.ownsSelected = null;
    const symbol = this.selectedResult?.instrumentSymbol;
    if (!this.accountId || !symbol) {
      return;
    }
    this.holdingService
      .getHoldingsByAccountId(this.accountId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (holdings) => {
          if (symbol === this.selectedResult?.instrumentSymbol) {
            this.ownsSelected = holdings.some(
              (h) => h.instrumentSymbol === symbol,
            );
          }
        },
        error: (error) => console.error("Could not load holdings", error),
      });
  }

  private isPreviewRejected(side: "BUY" | "SELL"): boolean {
    const preview = this.preview;
    const matchesInput =
      preview &&
      preview.side === side &&
      preview.instrumentSymbol === this.selectedResult?.instrumentSymbol &&
      preview.quantityMode === this.quantityMode &&
      (this.quantityMode === "AMOUNT"
        ? Number(preview.requestedAmount) === this.amount
        : Number(preview.quantity) === this.quantity);

    if (preview && matchesInput && !preview.valid) {
      const reason = preview.reason ?? "";
      if (
        (side === "BUY" && reason.startsWith("Insufficient cash available:")) ||
        (side === "SELL" &&
          (reason.startsWith("Insufficient quantity:") ||
            reason.startsWith("Account does not hold ")))
      ) {
        return false;
      }
      this.errorMessage = preview.reason ?? "Order is not valid.";
      return true;
    }
    return false;
  }

  private requestPreview(): void {
    this.previewTrigger$.next();
  }

  private buildPreviewRequest(): TradeOrderPreviewRequest | null {
    if (!this.accountId || !this.selectedResult || !this.hasOrderInput) {
      return null;
    }

    return {
      side: this.isBuy ? "BUY" : "SELL",
      instrumentSymbol: this.selectedResult.instrumentSymbol,
      quantityMode: this.quantityMode,
      ...(this.quantityMode === "AMOUNT"
        ? { amount: this.amount }
        : { quantity: this.quantity }),
      accountId: this.accountId,
    };
  }

  private applyQuantityModeConstraints(requested: QuantityMode): void {
    let mode = requested;
    if (mode === "SHARES" && !this.canTradeWholeShares) {
      mode = "AMOUNT";
    } else if (mode === "AMOUNT" && !this.canTradeByAmount) {
      mode = "SHARES";
    }
    if (mode === this.quantityMode) {
      return;
    }
    this.quantityMode = mode;
    this.quantity = 0;
    this.amount = 0;
    this.amountInputValid = false;
    this.preview = null;
    this.clearErrorMessage();
    this.requestPreview();
  }

  private formatUsd(value: number): string {
    return value.toLocaleString("en-US", {
      style: "currency",
      currency: "USD",
    });
  }

  get searchAssetTypes(): string[] {
    return TAG_ASSET_TYPES[this.selectedTag];
  }

  get searchMarket(): string {
    return this.selectedTag === "Foreign" ? "FOREIGN" : "US";
  }

  get canTradeWholeShares(): boolean {
    return (
      this.selectedTag !== "Crypto" &&
      (this.selectedResult?.wholeSharesAllowed ?? true)
    );
  }

  get canTradeByAmount(): boolean {
    return this.selectedResult?.fractionalAllowed ?? true;
  }

  get hasOrderInput(): boolean {
    return this.quantityMode === "AMOUNT"
      ? this.amountInputValid && this.amount > 0
      : this.quantity > 0;
  }

  get estimatedQuantity(): number | null {
    if (this.quantityMode === "SHARES") {
      return this.quantity || null;
    }
    if (!this.isPreviewForCurrentInput() || this.preview?.quantity == null) {
      return null;
    }
    return this.preview.quantity;
  }

  private isPreviewForCurrentInput(): boolean {
    return (
      !!this.preview &&
      this.preview.side === (this.isBuy ? "BUY" : "SELL") &&
      this.preview.instrumentSymbol === this.selectedResult?.instrumentSymbol &&
      this.preview.quantityMode === this.quantityMode &&
      (this.quantityMode === "AMOUNT"
        ? Number(this.preview.requestedAmount) === this.amount
        : Number(this.preview.quantity) === this.quantity)
    );
  }

  get isMarketClosed(): boolean {
    return this.selectedResult?.marketOpen === false;
  }

  private loadCashAvailable(): void {
    if (!this.accountId) {
      return;
    }
    this.accountService
      .getAccountBalance(this.accountId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (balance) => (this.cashAvailable = balance.cashAvailable),
        error: (error) =>
          console.error("Could not load account balance", error),
      });
  }

  get estimatedTotal(): number | null {
    if (this.preview?.estimatedValue != null) {
      return this.preview.estimatedValue;
    }
    if (this.quantityMode === "AMOUNT") {
      return this.amount > 0 ? this.amount : null;
    }
    return this.selectedResult && this.quantity > 0
      ? this.selectedResult.currentPrice * this.quantity
      : null;
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

  get buyingPowerLabel(): string {
    if (!this.preview) {
      return "Available buying power";
    }
    return this.isBuy ? "Available buying power" : "Cash after sale";
  }

  get selectedCurrency(): string {
    return (
      this.selectedResult?.currencyCode ??
      this.preview?.instrumentCurrency ??
      "USD"
    );
  }

  private formatErrorMessage(message: string | null): string | null {
    if (!message) return null;
    return message.replace(/\b(\d+)\.0+\b/g, "$1");
  }
}
