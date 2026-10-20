import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AccountService } from '@app/services/account.service';
import { BalanceResponse } from '@app/models/balance-response.model';
import { FundsAction } from '@app/models/funds-request.model';

@Component({
  selector: 'app-funds',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, FormsModule],
  templateUrl: './funds.component.html',
  styleUrl: './funds.component.scss'
})
export class FundsComponent implements OnInit {
  readonly maxAmount = 1000000;
  accountId = '';
  balance: BalanceResponse | null = null;
  amount = '';
  selectedAction: FundsAction = 'ADD';
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  constructor(private accountService: AccountService) {}

  ngOnInit(): void {
    this.accountService.getMyAccount().subscribe({
      next: account => {
        this.accountId = account.accountId;
        this.loadBalance();
      },
      error: () => this.errorMessage = 'Could not load your account.'
    });
  }

  selectAction(action: FundsAction): void {
    this.selectedAction = action;
    this.successMessage = '';
    this.errorMessage = '';
  }

  onAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.match(/^\d*(?:\.\d{0,2})?/)?.[0] ?? '';
    if (value.startsWith('.')) {
      value = `0${value}`;
    }
    if (Number(value) > this.maxAmount) {
      value = String(this.maxAmount);
    }
    input.value = value;
    this.amount = value;
  }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    const amountText = this.amount.trim();
    const numericAmount = Number(amountText);
    const decimalDigits = amountText.includes('.') ? amountText.split('.')[1].length : 0;
    const hasValidFormat = /^\d+(?:\.\d+)?$/.test(amountText);

    if (!hasValidFormat || decimalDigits > 2) {
      this.errorMessage = 'Enter a valid amount with up to two decimal places.';
      return;
    }

    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      this.errorMessage = 'Enter an amount greater than zero.';
      return;
    }

    if (numericAmount > this.maxAmount) {
      this.errorMessage = `The maximum amount per transaction is ${this.maxAmount.toLocaleString()}.`;
      return;
    }

    if (this.selectedAction === 'WITHDRAW' && this.balance && numericAmount > this.balance.cashAvailable) {
      this.errorMessage = 'You cannot withdraw more than your available cash.';
      return;
    }

    this.isSubmitting = true;
    this.accountService.updateFunds(this.accountId, {
      amount: numericAmount,
      action: this.selectedAction
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.amount = '';
        this.successMessage = this.selectedAction === 'ADD' ? 'Funds added successfully.' : 'Funds withdrawn successfully.';
        this.loadBalance();
      },
      error: error => {
        this.isSubmitting = false;
        this.errorMessage = error?.error?.message || 'We could not update your funds.';
      }
    });
  }

  private loadBalance(): void {
    this.accountService.getAccountBalance(this.accountId).subscribe({
      next: balance => this.balance = balance,
      error: () => this.errorMessage = 'Could not load your current balance.'
    });
  }
}
