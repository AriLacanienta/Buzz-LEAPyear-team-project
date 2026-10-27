import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ChatbotComponent } from './chatbot.component';
import { ChatbotService } from '../../../services/chatbot.service';
import { DisputeService } from '../../../services/dispute.service';
import { DisputeResponse } from '../../../models/dispute.model';
import { RecentTradeOrderResponse, SpringPage } from '../../../models/recent-trade-order-response.model';
import { AccountService } from '../../../services/account.service';
import { DisputeComposerService } from '../../../services/dispute-composer.service';
import { ChatResponse } from '../../../models/chatbot.model';

describe('ChatbotComponent inline disputes', () => {
  let fixture: ComponentFixture<ChatbotComponent>;
  let component: ChatbotComponent;
  let chatbot: jasmine.SpyObj<ChatbotService>;
  let disputes: jasmine.SpyObj<DisputeService>;
  let accounts: jasmine.SpyObj<AccountService>;
  let composer: DisputeComposerService;
  const filledOrder = {
    orderId: 7, instrumentSymbol: 'AAPL', side: 'BUY', quantity: 5, price: 20,
    orderDate: '2026-10-22T11:00:00', status: 'FILLED', timeUpdated: null, reasonText: null,
    hasOpenDispute: false
  } as RecentTradeOrderResponse;
  const disputedOrder = { ...filledOrder, orderId: 9, hasOpenDispute: true };
  const rejectedOrder = { ...filledOrder, orderId: 8, status: 'REJECTED' } as RecentTradeOrderResponse;
  const page: SpringPage<RecentTradeOrderResponse> = {
    content: [filledOrder, disputedOrder, rejectedOrder], number: 0, size: 10, totalElements: 3, totalPages: 1, first: true, last: true
  };
  const result: DisputeResponse = {
    id: 12, tradeOrderId: 7, accountId: 3, tradeValue: 100,
    message: 'Incorrect Price', status: 'OPEN', severity: 'NORMAL',
    createdAt: '2026-10-22T12:00:00', resolvedAt: null
  };

  beforeEach(async () => {
    chatbot = jasmine.createSpyObj('ChatbotService', ['sendMessage']);
    disputes = jasmine.createSpyObj('DisputeService', ['create']);
    accounts = jasmine.createSpyObj('AccountService', ['getMyAccount', 'getRecentTradeOrders']);
    accounts.getMyAccount.and.returnValue(of({ accountId: '3', accountName: 'My account' }));
    accounts.getRecentTradeOrders.and.returnValue(of(page));
    chatbot.sendMessage.and.returnValue(of({ intent: 'DISPUTE_FORM', message: 'Raise a dispute', data: null, action: 'CREATE_DISPUTE' }));
    await TestBed.configureTestingModule({
      imports: [ChatbotComponent],
      providers: [
        { provide: ChatbotService, useValue: chatbot },
        { provide: DisputeService, useValue: disputes },
        { provide: AccountService, useValue: accounts },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ChatbotComponent);
    component = fixture.componentInstance;
    composer = TestBed.inject(DisputeComposerService);
    component.isOpen = true;
  });

  function openForm() {
    component.message = 'raise a dispute';
    component.sendMessage();
    fixture.detectChanges();
    return component.activeDispute!;
  }

  function reviewForm() {
    const card = openForm();
    card.selectedOrder = card.orders[0];
    card.description = '  Incorrect Price  ';
    component.reviewDispute(card);
    return card;
  }

  it('renders an inline form without submitting, and keeps it separate from the composer', () => {
    openForm();
    const forms = fixture.nativeElement.querySelectorAll('form');
    expect(forms.length).toBe(2);
    expect(fixture.nativeElement.querySelector('form form')).toBeNull();
    expect(fixture.nativeElement.querySelector('textarea')).not.toBeNull();
    expect(disputes.create).not.toHaveBeenCalled();
    expect(chatbot.sendMessage).not.toHaveBeenCalled();
  });

  it('opens the form from the shortcut without requiring chatbot intent support', () => {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.chatbot-shortcuts button').click();
    fixture.detectChanges();
    expect(component.activeDispute?.state).toBe('draft');
    expect(component.messages.some(message => message.role === 'user' && message.text === 'raise a dispute')).toBeTrue();
    expect(chatbot.sendMessage).not.toHaveBeenCalled();
    expect(disputes.create).not.toHaveBeenCalled();
  });

  it('also handles a dispute intent returned by the backend', () => {
    component.message = 'I have an issue';
    component.sendMessage();
    expect(chatbot.sendMessage).toHaveBeenCalledWith('I have an issue');
    expect(component.activeDispute?.state).toBe('draft');
  });

  it('requests and renders a portfolio summary from the shortcut without clearing typed text', () => {
    const portfolio = {
      holdingsCount: 2, portfolioValue: 100, totalCostBasis: 80,
      todayChange: 5, todayChangePercent: 5, allTimeChange: 20, allTimeChangePercent: 25
    };
    chatbot.sendMessage.and.returnValue(of({ intent: 'PORTFOLIO', message: 'Your portfolio summary', data: portfolio, action: 'VIEW_PORTFOLIO' }));
    component.message = 'My unfinished question';
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.portfolio-summary-shortcut').click();
    fixture.detectChanges();
    expect(chatbot.sendMessage).toHaveBeenCalledOnceWith('portfolio summary');
    expect(component.message).toBe('My unfinished question');
    expect(fixture.nativeElement.querySelector('.portfolio-summary').textContent).toContain('$100.00');
    expect(component.loading).toBeFalse();
    expect(disputes.create).not.toHaveBeenCalled();
  });

  it('disables the summary shortcut while a request is pending and restores it on failure', () => {
    const pending = new Subject<ChatResponse>();
    chatbot.sendMessage.and.returnValue(pending);
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.portfolio-summary-shortcut');
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBeTrue();
    component.sendMessage('portfolio summary');
    expect(chatbot.sendMessage).toHaveBeenCalledTimes(1);
    pending.error(new HttpErrorResponse({ status: 503 }));
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('I could not process that request.');
  });

  it('reuses the active draft and retains it when minimized', () => {
    const card = openForm();
    card.description = 'Keep this';
    openForm();
    component.toggleChatbot();
    fixture.detectChanges();
    component.toggleChatbot();
    fixture.detectChanges();
    expect(component.activeDispute).toBe(card);
    expect(card.description).toBe('Keep this');
    expect(component.messages.filter(message => message.dispute).length).toBe(1);
  });

  it('rejects missing or ineligible selections, whitespace descriptions and oversized descriptions', () => {
    const card = openForm();
    card.description = 'Problem';
    for (const order of [undefined, rejectedOrder, { ...filledOrder, orderId: 0 }]) {
      card.selectedOrder = order;
      component.reviewDispute(card);
      expect(card.state).toBe('draft');
      expect(card.error).toBeTruthy();
    }
    card.selectedOrder = filledOrder;
    for (const description of ['   ', 'x'.repeat(2001)]) {
      card.description = description;
      component.reviewDispute(card);
      expect(card.state).toBe('draft');
      expect(card.error).toBeTruthy();
    }
    expect(disputes.create).not.toHaveBeenCalled();
  });

  it('loads only filled orders without an open dispute using the authenticated account', () => {
    const card = openForm();
    expect(accounts.getMyAccount).toHaveBeenCalled();
    expect(accounts.getRecentTradeOrders).toHaveBeenCalledWith('3', 0, 10);
    expect(card.orders).toEqual([filledOrder]);
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    expect(select.options.length).toBe(2);
    expect(select.options[1].textContent).toContain('#7');
    expect(select.options[1].textContent).toContain('AAPL');
    expect(fixture.nativeElement.querySelector('input[name="orderId"]')).toBeNull();
  });

  it('does not open a dispute form for an order that already has an open dispute', () => {
    component.openDisputeForm(disputedOrder);
    expect(component.activeDispute).toBeUndefined();
  });

  it('replies with a plain message when no eligible dispute orders exist', () => {
    accounts.getRecentTradeOrders.and.returnValue(of({
      ...page, content: [disputedOrder, rejectedOrder], totalElements: 2, totalPages: 1
    }));

    const card = openForm();

    expect(component.activeDispute).toBeUndefined();
    expect(component.messages.at(-1)?.text).toBe('No eligible executed orders found.');
    expect(component.messages.at(-1)?.dispute).toBeUndefined();
    expect(fixture.nativeElement.querySelector('.dispute-form')).toBeNull();
  });

  it('opens the minimized chatbot with a row action preselected without creating a dispute', () => {
    component.isOpen = false;
    composer.openForOrder(filledOrder);
    fixture.detectChanges();
    expect(component.isOpen).toBeTrue();
    expect(component.activeDispute?.selectedOrder?.orderId).toBe(7);
    expect(fixture.nativeElement.querySelector('.dispute-order-summary').textContent).toContain('AAPL');
    expect(disputes.create).not.toHaveBeenCalled();
  });

  it('keeps an existing draft instead of overwriting it from another row', () => {
    const card = reviewForm();
    composer.openForOrder({ ...filledOrder, orderId: 99 });
    expect(component.activeDispute).toBe(card);
    expect(card.selectedOrder?.orderId).toBe(7);
    expect(card.description).toBe('Incorrect Price');
    expect(card.error).toContain('Finish or cancel');
  });

  it('uses a row selection in an existing unselected draft without losing the description', () => {
    const card = openForm();
    card.description = 'Keep my description';
    composer.openForOrder(filledOrder);
    expect(component.activeDispute).toBe(card);
    expect(card.selectedOrder?.orderId).toBe(7);
    expect(card.description).toBe('Keep my description');
    expect(card.orders.filter(order => order.orderId === 7).length).toBe(1);
  });

  it('paginates order history and keeps an order selected from another page', () => {
    accounts.getRecentTradeOrders.and.returnValue(of({ ...page, totalPages: 2, last: false }));
    const card = openForm();
    card.selectedOrder = card.orders[0];
    const olderOrder = { ...filledOrder, orderId: 2, instrumentSymbol: 'MSFT' };
    accounts.getRecentTradeOrders.and.returnValue(of({ ...page, content: [olderOrder], number: 1, totalPages: 2 }));
    component.loadDisputeOrders(card, 1);
    expect(accounts.getRecentTradeOrders).toHaveBeenCalledWith('3', 1, 10);
    expect(card.pageIndex).toBe(1);
    expect(card.orders.map(order => order.orderId)).toEqual([7, 2]);
    expect(card.selectedOrder?.orderId).toBe(7);
  });

  it('handles empty or non-filled order pages without allowing review', () => {
    accounts.getRecentTradeOrders.and.returnValue(of({ ...page, content: [rejectedOrder], totalPages: 2 }));
    const card = openForm();
    expect(card.orders).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('No eligible executed orders found.');
    expect(fixture.nativeElement.querySelector('.dispute-actions button').disabled).toBeTrue();
    accounts.getRecentTradeOrders.and.returnValue(of({ ...page, content: [], totalElements: 0, totalPages: 0 }));
    component.loadDisputeOrders(card);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No eligible executed orders found');
  });

  it('shows a retryable order-load error and preserves a row-preselected order', () => {
    accounts.getRecentTradeOrders.and.returnValue(throwError(() => new HttpErrorResponse({ status: 503 })));
    composer.openForOrder(filledOrder);
    fixture.detectChanges();
    const card = component.activeDispute!;
    expect(card.ordersError).toContain('Could not load');
    expect(card.ordersLoading).toBeFalse();
    expect(card.selectedOrder?.orderId).toBe(7);
    expect(card.orders[0]).toBe(card.selectedOrder!);
    accounts.getRecentTradeOrders.and.returnValue(of(page));
    component.loadDisputeOrders(card);
    expect(card.ordersError).toBe('');
    expect(card.orders).toEqual([filledOrder]);
  });

  it('does not overwrite a new draft when an old cancelled draft finishes loading', () => {
    const pending = new Subject<SpringPage<RecentTradeOrderResponse>>();
    accounts.getRecentTradeOrders.and.returnValue(pending);
    const oldCard = openForm();
    component.cancelDispute(oldCard);
    accounts.getRecentTradeOrders.and.returnValue(of(page));
    const newCard = openForm();
    pending.next({ ...page, content: [] });
    pending.complete();
    expect(component.activeDispute).toBe(newCard);
    expect(newCard.orders).toEqual([filledOrder]);
  });

  it('reviews trimmed original-case text and supports edit and cancellation without a POST', () => {
    const card = reviewForm();
    expect(card.state).toBe('review');
    expect(card.description).toBe('Incorrect Price');
    component.editDispute(card);
    expect(card.state).toBe('draft');
    component.cancelDispute(card);
    expect(card.state).toBe('cancelled');
    expect(component.activeDispute).toBeUndefined();
    component.confirmDispute(card);
    expect(disputes.create).not.toHaveBeenCalled();
  });

  it('submits once on confirmation, disables duplicate submission and renders the result', () => {
    const pending = new Subject<DisputeResponse>();
    disputes.create.and.returnValue(pending);
    const card = reviewForm();
    component.confirmDispute(card);
    component.confirmDispute(card);
    component.cancelDispute(card);
    fixture.detectChanges();
    expect(card.state).toBe('submitting');
    expect(disputes.create).toHaveBeenCalledOnceWith({ tradeOrderId: 7, message: 'Incorrect Price' });
    expect(fixture.nativeElement.querySelector('.dispute-actions button').disabled).toBeTrue();
    pending.next(result);
    pending.complete();
    fixture.detectChanges();
    expect(component.activeDispute).toBeUndefined();
    expect(fixture.nativeElement.querySelector('.dispute-success').textContent).toContain('Dispute #12');
    expect(card.state).toBe('succeeded');
  });

  for (const status of [400, 401, 403, 404, 409, 0, 500]) {
    it(`preserves the draft and displays an error for HTTP ${status}`, () => {
      disputes.create.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      const card = reviewForm();
      component.confirmDispute(card);
      fixture.detectChanges();
      expect(card.state).toBe('review');
      expect(card.description).toBe('Incorrect Price');
      expect(card.error).toBeTruthy();
      expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
      expect(component.activeDispute).toBe(card);
      expect(disputes.create).toHaveBeenCalledTimes(1);
    });
  }

  it('preserves the existing chatbot card handling', () => {
    chatbot.sendMessage.and.returnValue(of({
      intent: 'PORTFOLIO', message: 'Your portfolio', action: 'VIEW_PORTFOLIO', data: { portfolioValue: 100 }
    }));
    component.message = 'portfolio';
    component.sendMessage();
    expect(component.messages.at(-1)?.portfolio?.portfolioValue).toBe(100);
    expect(component.activeDispute).toBeUndefined();
  });
});