  import { Component, DestroyRef, OnInit } from '@angular/core';
import { SearchComponent } from '../search/search.component';
import { SearchResultDetailComponent } from '../search-result-detail/search-result-detail.component';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { InstrumentSearchResponse } from '@app/models/instrument-search-response.model';
import { AccountService } from '@app/services/account.service';
import { BalanceResponse } from '@app/models/balance-response.model';
import { switchMap, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

const POLL_INTERVAL_MS = 3000;

type OrderType = 'buy' | 'sell';

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
  errorMessage: string = '';
  selectedTag: string = 'All';
  selectedResult: InstrumentSearchResponse | null = null;
  instruments: InstrumentSearchResponse[] | null = null;
  buyingPower: number = 0;

  constructor(private accountService: AccountService, private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.fetchBuyingPower();
  }

  fetchBuyingPower(): void {
    this.accountService.getMyAccount().pipe(
      switchMap(account => this.accountService.getAccountBalance(account.accountId)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (balance: BalanceResponse) => this.buyingPower = balance.cashAvailable,
      error: error => console.error('Could not load buying power', error)
    });
  }

  toggleOrderType(side: OrderType): void {
    this.isBuy = side === 'buy';
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
  }

  onSearchItemSelected(item: InstrumentSearchResponse | null): void {
    this.selectedResult = item;
    console.log('Selected item from search:', item);
    // Handle selected item (populate quantity, set symbol, etc.)
  }

  onPlaceOrder(): void {
    this.errorMessage = '';

    if (!this.selectedResult) {
      this.errorMessage = 'Please select an instrument.';
      return;
    }

    if (!Number.isInteger(this.quantity) || this.quantity <= 0) {
      this.errorMessage = 'Please enter a valid quantity greater than 0.';
      return;
    }

    this.isBuy ? this.handleBuy() : this.handleSell();
  }

  handleBuy(): void {
    // check if account has sufficient balance before placing a buy order
    // If balance is insufficient, set this.errorMessage and return.
    // Proceed with placing the buy order if balance is sufficient.
    this.submitOrder('buy');
  }

  handleSell(): void {
    // check if account has sufficient holdings before placing a sell order
    // If holdings are insufficient, set this.errorMessage and return.
    // Proceed with placing the sell order if holdings are sufficient.
    this.submitOrder('sell');
  }

  submitOrder(side: OrderType): void {

  }
}
