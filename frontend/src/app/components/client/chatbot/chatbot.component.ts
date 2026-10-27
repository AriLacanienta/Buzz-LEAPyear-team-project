import { AfterViewChecked, Component, DestroyRef, ElementRef, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, switchMap } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ChatMessage, StockComparisonResponse, TopMoversResponse } from '../../../models/chatbot.model';
import { TradeOrderPreviewResponse } from '../../../models/trade-order.model';
import { PortfolioSummaryResponse } from '../../../models/portfolio-summary-response.model';
import { DisputeCard } from '../../../models/dispute.model';
import { DisputeService } from '../../../services/dispute.service';
import { AccountService } from '../../../services/account.service';
import { RecentTradeOrderResponse } from '../../../models/recent-trade-order-response.model';
import { ChatbotService } from '@app/services/chatbot.service';
import { DisputeComposerService } from '@app/services/dispute-composer.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.scss']
})
export class ChatbotComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer?: ElementRef<HTMLDivElement>;

  message = '';
  loading = false;
  isOpen = false;
  private shouldScrollToBottom = false;
  private shouldFocusDispute = false;
  private readonly destroyRef = inject(DestroyRef);
  activeDispute?: DisputeCard;

  messages: ChatMessage[] = [
    {
      role: 'assistant',
      text: "Hi, I'm Buzz. I'm here to help with anything related to your account.\n\nI can help with:\n- Portfolio summaries\n- Stock comparisons (e.g., AAPL vs MSFT)\n- Top gainers/losers\n- Quotes (e.g., quote AAPL)\n- Trade (e.g., buy 5 shares of AAPL)\n- Trade disputes (e.g., raise a dispute)"
    }
  ];

  constructor(
    private chatbotService: ChatbotService,
    private router: Router,
    private disputeService: DisputeService,
    private accountService: AccountService,
    private disputeComposer: DisputeComposerService
  ) {
    this.disputeComposer.requests$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(order => {
      this.isOpen = true;
      this.openDisputeForm(order);
    });
  }

  toggleChatbot(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.shouldScrollToBottom = true;
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom && this.messagesContainer) {
      this.messagesContainer.nativeElement.scrollTop =
        this.messagesContainer.nativeElement.scrollHeight;
      this.shouldScrollToBottom = false;
    }
    if (this.shouldFocusDispute && this.messagesContainer) {
      const card = this.messagesContainer.nativeElement.querySelector<HTMLElement>('[data-active-dispute="true"]');
      card?.scrollIntoView({ block: 'nearest' });
      card?.querySelector<HTMLElement>('select:not(:disabled), textarea, button:not(:disabled)')?.focus({ preventScroll: true });
      this.shouldFocusDispute = false;
    }
  }

  sendMessage(shortcut?: string): void {
    const text = (shortcut ?? this.message).trim();

    if (!text || this.loading) {
      return;
    }

    this.messages.push({ role: 'user', text });
    if (shortcut === undefined) this.message = '';
    // Opening a form does not require an API call. Older running backends may
    // not know this intent yet; ownership is still enforced when submitting.
    if (/\bdisputes?\b/i.test(text)) {
      this.openDisputeForm();
      return;
    }
    this.loading = true;
    this.shouldScrollToBottom = true;

    this.chatbotService.sendMessage(text).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        if (response.intent === 'DISPUTE_FORM') {
          this.openDisputeForm();
          this.loading = false;
          this.shouldScrollToBottom = true;
          return;
        }

        let preview: TradeOrderPreviewResponse | undefined;

        if (response.intent === 'TRADE_PREVIEW') {
          preview = response.data as TradeOrderPreviewResponse;
        }

        const portfolio = response.intent === 'PORTFOLIO'
          ? response.data as PortfolioSummaryResponse
          : undefined;

        const marketMovers = response.intent === 'MARKET_MOVERS'
          ? response.data as TopMoversResponse
          : undefined;

        const comparison = response.intent === 'COMPARISON'
          ? response.data as StockComparisonResponse
          : undefined;

        this.messages.push({
          role: 'assistant',
          text: response.message,
          action: response.action,
          preview,
          portfolio,
          marketMovers,
          comparison
        });

        this.loading = false;
        this.shouldScrollToBottom = true;
      },
      error: () => {
        this.messages.push({
          role: 'assistant',
          text: 'I could not process that request.'
        });

        this.loading = false;
        this.shouldScrollToBottom = true;
      }
    });
  }

  openDisputeForm(order?: RecentTradeOrderResponse): void {
    if (order && (order.status !== 'FILLED' || order.hasOpenDispute)) return;
    if (!this.activeDispute) {
      const selectedOrder = order ? { ...order } : undefined;
      this.activeDispute = {
        state: 'draft', selectedOrder,
        orders: selectedOrder ? [selectedOrder] : [], ordersLoading: false, ordersError: '',
        pageIndex: 0, totalPages: 0, pageOrderCount: 0, description: '', error: ''
      };
      this.messages.push({
        role: 'assistant',
        text: 'Select an executed order and describe the issue. Review before confirming. Only orders belonging to your signed-in account can be disputed.',
        dispute: this.activeDispute
      });
      this.loadDisputeOrders(this.activeDispute);
    } else if (order && this.activeDispute.state === 'draft' && !this.activeDispute.selectedOrder) {
      const selected = this.activeDispute.orders.find(option => option.orderId === order.orderId) ?? { ...order };
      this.activeDispute.selectedOrder = selected;
      if (!this.activeDispute.orders.includes(selected)) this.activeDispute.orders.unshift(selected);
      this.activeDispute.error = '';
    } else if (order && order.orderId !== this.activeDispute.selectedOrder?.orderId) {
      this.activeDispute.error = 'Finish or cancel your current dispute before starting one for a different order. Your draft has been kept.';
    }
    this.shouldScrollToBottom = true;
    this.shouldFocusDispute = true;
  }

  loadDisputeOrders(card: DisputeCard, page = 0): void {
    if (card !== this.activeDispute || card.state !== 'draft' || card.ordersLoading || page < 0) return;
    card.ordersLoading = true;
    card.ordersError = '';
    this.accountService.getMyAccount().pipe(
      switchMap(account => this.accountService.getRecentTradeOrders(account.accountId, page, 10)),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => card.ordersLoading = false)
    ).subscribe({
      next: response => {
        if (card !== this.activeDispute) return;
        const orders = response.content.filter(order => order.status === 'FILLED' && !order.hasOpenDispute);
        card.pageOrderCount = orders.length;
        card.pageIndex = response.number;
        card.totalPages = response.totalPages;
        // Keep a preselected order available even when it lives on another page.
        if (card.selectedOrder) {
          const selected = orders.find(order => order.orderId === card.selectedOrder?.orderId);
          if (selected) card.selectedOrder = selected;
          else {
            const selectedInResponse = response.content.find(order => order.orderId === card.selectedOrder?.orderId);
            if (selectedInResponse && (selectedInResponse.status !== 'FILLED' || selectedInResponse.hasOpenDispute)) {
              card.selectedOrder = undefined;
            } else {
              orders.unshift(card.selectedOrder);
            }
          }
        }
        card.orders = orders;
        if (!orders.length && !card.selectedOrder && response.totalPages <= 1) {
          const message = this.messages.find(item => item.dispute === card);
          if (message) {
            message.text = 'No eligible executed orders found.';
            delete message.dispute;
          }
          this.activeDispute = undefined;
        }
        this.shouldScrollToBottom = true;
      },
      error: (error: HttpErrorResponse) => {
        if (card !== this.activeDispute) return;
        card.ordersError = error.status === 401
          ? 'Your session has expired. Sign in again to load orders.'
          : 'Could not load your order history. Retry, or select Raise dispute on an executed order in Recent Orders.';
      }
    });
  }

  trackDisputeOrder(_: number, order: RecentTradeOrderResponse): number {
    return order.orderId;
  }

  reviewDispute(card: DisputeCard): void {
    if (card !== this.activeDispute || card.state !== 'draft') return;
    card.error = '';
    const order = card.selectedOrder;
    if (!order || order.status !== 'FILLED' || !Number.isSafeInteger(order.orderId) || order.orderId <= 0) {
      card.error = 'Select a successfully executed order from your order history.';
      return;
    }
    const description = card.description.trim();
    if (!description || card.description.length > 2000) {
      card.error = 'Enter a description between 1 and 2,000 characters.';
      return;
    }
    card.description = description;
    card.state = 'review';
    this.shouldScrollToBottom = true;
  }

  editDispute(card: DisputeCard): void {
    if (card === this.activeDispute && card.state === 'review') {
      card.state = 'draft';
      card.error = '';
    }
  }

  cancelDispute(card: DisputeCard): void {
    if (card === this.activeDispute && (card.state === 'draft' || card.state === 'review')) {
      card.state = 'cancelled';
      card.error = '';
      this.activeDispute = undefined;
    }
  }

  confirmDispute(card: DisputeCard): void {
    if (card !== this.activeDispute || card.state !== 'review' || !card.selectedOrder) return;
    card.state = 'submitting';
    card.error = '';
    this.disputeService.create({ tradeOrderId: card.selectedOrder.orderId, message: card.description }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        if (card.state === 'submitting') card.state = 'review';
        this.shouldScrollToBottom = true;
      })
    ).subscribe({
      next: result => {
        card.result = result;
        card.state = 'succeeded';
        this.activeDispute = undefined;
      },
      error: (error: HttpErrorResponse) => {
        const messages: Record<number, string> = {
          400: 'Check the selected order and description, then edit your dispute.',
          401: 'Your session has expired. Sign in again before submitting.',
          403: 'You can only dispute orders belonging to your signed-in account.',
          404: 'That trade order is no longer available. Edit your dispute and select another order.',
          409: 'This order is not eligible: it must have executed successfully and must not already have an open dispute.'
        };
        card.error = messages[error.status] ?? 'The submission could not be confirmed. Check your existing disputes before retrying to avoid a duplicate.';
      }
    });
  }

  openAction(action: string | null, preview?: TradeOrderPreviewResponse): void {
    if (action === 'VIEW_PORTFOLIO') {
      this.router.navigate(['/portfolio']);
    }

    if (action === 'VIEW_TRADE') {
      this.router.navigate(['/trade'], {
        queryParams: preview
          ? {
              side: preview.side,
              symbol: preview.instrumentSymbol,
              quantity: preview.quantity,
              confirm: true
            }
          : undefined
      });
    }
  }
}
