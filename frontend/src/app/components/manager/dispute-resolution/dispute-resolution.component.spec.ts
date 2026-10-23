import { DIALOG_DATA, Dialog, DialogModule, DialogRef } from '@angular/cdk/dialog';
import { OverlayContainer } from '@angular/cdk/overlay';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { Dispute } from '../../../models/dispute.model';
import { DisputeService } from '../../../services/dispute.service';
import { DisputeResolutionComponent } from './dispute-resolution.component';

describe('DisputeResolutionComponent', () => {
  let fixture: ComponentFixture<DisputeResolutionComponent>;
  let service: jasmine.SpyObj<DisputeService>;
  let dialogRef: { close: jasmine.Spy; disableClose: boolean };
  const resolved: Dispute = {
    id: 7, clientName: 'Alex Smith', accountId: 12, tradeOrderId: 1, tradeValue: 100,
    message: 'Original', status: 'RESOLVED', severity: 'NORMAL',
    createdAt: '2026-10-22T12:00:00', resolvedAt: '2026-10-23T12:00:00',
    resolutionMessage: 'Reviewed'
  };

  beforeEach(() => {
    service = jasmine.createSpyObj<DisputeService>('DisputeService', ['resolve']);
    service.resolve.and.returnValue(of(resolved));
    dialogRef = { close: jasmine.createSpy('close'), disableClose: false };
    TestBed.configureTestingModule({
      imports: [DisputeResolutionComponent],
      providers: [
        { provide: DisputeService, useValue: service },
        { provide: DIALOG_DATA, useValue: 7 },
        { provide: DialogRef, useValue: dialogRef }
      ]
    });
    fixture = TestBed.createComponent(DisputeResolutionComponent);
    fixture.detectChanges();
  });

  for (const text of ['', ' \n\t ', 'x'.repeat(2001)]) {
    it(`blocks invalid resolution text of length ${text.length}`, () => {
      fixture.componentInstance.resolutionMessage.setValue(text);
      fixture.componentInstance.resolve();
      fixture.detectChanges();
      expect(service.resolve).not.toHaveBeenCalled();
      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(fixture.nativeElement.textContent).toContain('not just whitespace');
    });
  }

  it('accepts exactly 2000 characters', () => {
    fixture.componentInstance.resolutionMessage.setValue('x'.repeat(2000));
    fixture.componentInstance.resolve();
    expect(service.resolve).toHaveBeenCalledWith(7, 'x'.repeat(2000));
    expect(dialogRef.close).toHaveBeenCalledOnceWith(true);
  });

  it('prevents navigation and submits the selected ID and trimmed text from the rendered form', () => {
    const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    textarea.value = '  Reviewed  ';
    textarea.dispatchEvent(new Event('input'));
    const submit = new Event('submit', { bubbles: true, cancelable: true });
    fixture.nativeElement.querySelector('form').dispatchEvent(submit);
    expect(submit.defaultPrevented).toBeTrue();
    expect(service.resolve).toHaveBeenCalledOnceWith(7, 'Reviewed');
    expect(dialogRef.close).toHaveBeenCalledOnceWith(true);
  });

  it('cancels without calling the API', () => {
    fixture.nativeElement.querySelector('button[type="button"]').click();
    expect(dialogRef.close).toHaveBeenCalledOnceWith();
    expect(service.resolve).not.toHaveBeenCalled();
  });

  it('prevents duplicate requests, editing, and dismissal while saving', () => {
    const request = new Subject<Dispute>();
    service.resolve.and.returnValue(request);
    fixture.componentInstance.resolutionMessage.setValue('Reviewed');
    fixture.componentInstance.resolve();
    fixture.detectChanges();
    expect(dialogRef.disableClose).toBeTrue();
    expect(fixture.nativeElement.querySelector('textarea').readOnly).toBeTrue();
    expect(fixture.nativeElement.querySelector('button[type="button"]').disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    fixture.componentInstance.resolve();
    fixture.componentInstance.cancel();
    expect(service.resolve).toHaveBeenCalledTimes(1);
    expect(dialogRef.close).not.toHaveBeenCalled();
    request.next(resolved);
    request.complete();
    expect(dialogRef.close).toHaveBeenCalledOnceWith(true);
    expect(dialogRef.disableClose).toBeFalse();
  });

  it('preserves text and allows retry after failure', () => {
    service.resolve.and.returnValues(throwError(() => new Error('offline')), of(resolved));
    fixture.componentInstance.resolutionMessage.setValue('Reviewed');
    fixture.componentInstance.resolve();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Unable to resolve');
    expect(fixture.componentInstance.resolutionMessage.value).toBe('Reviewed');
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(dialogRef.disableClose).toBeFalse();
    expect(fixture.componentInstance.saving).toBeFalse();
    fixture.componentInstance.resolve();
    expect(service.resolve).toHaveBeenCalledTimes(2);
    expect(dialogRef.close).toHaveBeenCalledOnceWith(true);
    expect(fixture.componentInstance.error).toBe('');
  });
});

describe('Dispute resolution dialog dismissal', () => {
  let dialog: Dialog;
  let overlay: OverlayContainer;
  let service: jasmine.SpyObj<DisputeService>;

  beforeEach(() => {
    service = jasmine.createSpyObj<DisputeService>('DisputeService', ['resolve']);
    TestBed.configureTestingModule({
      imports: [DialogModule, DisputeResolutionComponent],
      providers: [{ provide: DisputeService, useValue: service }]
    });
    dialog = TestBed.inject(Dialog);
    overlay = TestBed.inject(OverlayContainer);
  });

  afterEach(() => {
    dialog.closeAll();
    overlay.ngOnDestroy();
  });

  for (const dismissal of ['Escape', 'backdrop']) {
    it(`dismisses with ${dismissal} while idle without submitting`, () => {
      const ref = dialog.open<boolean, number>(DisputeResolutionComponent, { data: 7 });
      const closed = jasmine.createSpy('closed');
      ref.closed.subscribe(closed);
      if (dismissal === 'Escape') {
        overlay.getContainerElement().querySelector('cdk-dialog-container')!
          .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, bubbles: true }));
      } else {
        (overlay.getContainerElement().querySelector('.cdk-overlay-backdrop') as HTMLElement).click();
      }
      expect(closed).toHaveBeenCalledWith(undefined);
      expect(service.resolve).not.toHaveBeenCalled();
    });
  }

  it('blocks Escape and backdrop dismissal until a failed request finishes', () => {
    const request = new Subject<Dispute>();
    service.resolve.and.returnValue(request);
    const ref = dialog.open<boolean, number, DisputeResolutionComponent>(
      DisputeResolutionComponent, { data: 7 }
    );
    const closed = jasmine.createSpy('closed');
    ref.closed.subscribe(closed);
    ref.componentInstance!.resolutionMessage.setValue('Reviewed');
    ref.componentInstance!.resolve();
    const container = overlay.getContainerElement();
    container.querySelector('cdk-dialog-container')!
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, bubbles: true }));
    (container.querySelector('.cdk-overlay-backdrop') as HTMLElement).click();
    expect(closed).not.toHaveBeenCalled();
    expect(service.resolve).toHaveBeenCalledTimes(1);
    request.error(new Error('offline'));
    expect(ref.componentInstance!.resolutionMessage.value).toBe('Reviewed');
    (container.querySelector('.cdk-overlay-backdrop') as HTMLElement).click();
    expect(closed).toHaveBeenCalledWith(undefined);
  });
});
