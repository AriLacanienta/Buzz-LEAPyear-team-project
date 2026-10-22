import { AfterViewChecked, Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ChatbotService } from '../../../services/chatbot.service';
import { ChatMessage, StockComparisonResponse, TopMoversResponse } from '../../../models/chatbot.model';
import { TradeOrderPreviewResponse } from '../../../models/trade-order.model';
import { PortfolioSummaryResponse } from '../../../models/portfolio-summary-response.model';

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

  messages: ChatMessage[] = [
    {
      role: 'assistant',
      text: "Hi, I'm Buzz. I'm here to help with anything related to your account.\n\nI can help with:\n- Portfolio summaries\n- Stock comparisons (e.g., AAPL vs MSFT)\n- Top gainers/losers\n- Quotes (e.g., quote AAPL)\n- Trade (e.g., buy 5 shares of AAPL)"
    }
  ];

  constructor(
    private chatbotService: ChatbotService,
    private router: Router
  ) {}

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
  }

  sendMessage(): void {
    const text = this.message.trim();

    if (!text || this.loading) {
      return;
    }

    this.messages.push({ role: 'user', text });
    this.message = '';
    this.loading = true;
    this.shouldScrollToBottom = true;

    this.chatbotService.sendMessage(text).subscribe({
      next: response => {
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