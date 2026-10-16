import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { AccountComponent } from '@app/pages/account/account.component';
import { LoginComponent } from '@app/pages/login/login.component';
import { MarketsComponent } from '@app/pages/markets/markets.component';
import { AccountService } from '@app/services/account.service';
import { AuthService } from '@app/services/auth.service';
import { InstrumentService } from '@app/services/instrument.service';
import { Router } from '@angular/router';

describe('API-consuming components', () => {
  describe('AccountComponent', () => {
    let fixture: ComponentFixture<AccountComponent>;
    const accountService = jasmine.createSpyObj('AccountService', ['getMyAccount', 'setAccountName']);

    beforeEach(async () => {
      accountService.getMyAccount.and.returnValue(of({ accountId: 7, accountName: 'Brokerage' }));
      await TestBed.configureTestingModule({
        declarations: [AccountComponent],
        providers: [{ provide: AccountService, useValue: accountService }]
      }).compileComponents();
      fixture = TestBed.createComponent(AccountComponent);
      fixture.detectChanges();
    });

    it('replaces the placeholder account name from GET /accounts/me', () => {
      expect(fixture.componentInstance.accountInfo.name).toBe('Brokerage');
      expect(accountService.setAccountName).toHaveBeenCalledWith('Brokerage');
    });
  });

  describe('LoginComponent', () => {
    let fixture: ComponentFixture<LoginComponent>;
    const authService = jasmine.createSpyObj('AuthService', ['login', 'hasRole']);
    const router = jasmine.createSpyObj('Router', ['navigate']);

    beforeEach(async () => {
      authService.login.and.returnValue(of({
        token: 'jwt',
        type: 'Bearer',
        username: 'user',
        email: 'user@example.com',
        roles: ['CLIENT']
      }));
      authService.hasRole.and.returnValue(false);
      await TestBed.configureTestingModule({
        declarations: [LoginComponent],
        providers: [
          { provide: AuthService, useValue: authService },
          { provide: Router, useValue: router }
        ]
      }).compileComponents();
      fixture = TestBed.createComponent(LoginComponent);
    });

    it('does not call the API for an invalid login form', () => {
      fixture.componentInstance.onSubmit();

      expect(authService.login).not.toHaveBeenCalled();
      expect(fixture.componentInstance.submitted).toBeTrue();
    });

    it('posts valid credentials and routes clients to the dashboard', () => {
      fixture.componentInstance.loginForm.setValue({
        email: 'user@example.com',
        password: 'secret'
      });
      fixture.componentInstance.onSubmit();

      expect(authService.login).toHaveBeenCalledWith('user@example.com', 'secret');
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
      expect(fixture.componentInstance.loading).toBeFalse();
    });
  });

  describe('MarketsComponent', () => {
    let fixture: ComponentFixture<MarketsComponent>;
    const instrumentService = jasmine.createSpyObj('InstrumentService', ['getInstruments']);

    beforeEach(async () => {
      instrumentService.getInstruments.and.returnValue(of({ content: [{ instrumentSymbol: 'AAPL' }] }));
      await TestBed.configureTestingModule({
        imports: [MarketsComponent],
        providers: [{ provide: InstrumentService, useValue: instrumentService }]
      }).compileComponents();
      fixture = TestBed.createComponent(MarketsComponent);
      fixture.detectChanges();
    });

    afterEach(() => fixture.componentInstance.ngOnDestroy());

    it('loads the default US equity market with the documented page size', () => {
      expect(instrumentService.getInstruments).toHaveBeenCalledWith('EQUITY', 'US', 0, 100);
      expect(fixture.componentInstance.instruments).toEqual([{ instrumentSymbol: 'AAPL' }]);
    });

    it('reloads with the selected market and asset type', () => {
      instrumentService.getInstruments.calls.reset();

      fixture.componentInstance.onMarketChange('UK');
      expect(instrumentService.getInstruments).toHaveBeenCalledWith('EQUITY', 'UK', 0, 100);

      instrumentService.getInstruments.calls.reset();
      fixture.componentInstance.onAssetTypeChange('BOND');
      expect(instrumentService.getInstruments).toHaveBeenCalledWith('BOND', 'UK', 0, 100);
    });
  });
});
