import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ManagerClient } from '../../../../models/manager-client.model';
import { DisputeService } from '../../../../services/dispute.service';
import { ClientDetailComponent } from './client-detail.component';

describe('ClientDetailComponent disputes', () => {
  it('loads the selected user disputes and updates the scope when another client is selected', () => {
    const service = jasmine.createSpyObj<DisputeService>('DisputeService', ['search']);
    service.search.and.returnValue(of({
      content: [], number: 0, size: 20, totalElements: 0, totalPages: 0
    }));
    TestBed.configureTestingModule({
      imports: [ClientDetailComponent],
      providers: [{ provide: DisputeService, useValue: service }]
    });
    const fixture = TestBed.createComponent(ClientDetailComponent);
    fixture.detectChanges();
    expect(service.search).not.toHaveBeenCalled();

    const client: ManagerClient = {
      id: 42, name: 'Alex Smith', email: 'alex@example.test', reference: 'CLT-0042',
      initials: 'AS', trades: [], rejectedOrders: 0, orderTotal: 0, totalValue: 0
    };
    fixture.componentRef.setInput('client', client);
    fixture.detectChanges();
    expect(service.search).toHaveBeenCalledOnceWith({
      userId: 42, page: 0, size: 20, sortBy: 'status', sortDirection: 'asc'
    });
    expect(fixture.nativeElement.querySelector('app-manager-disputes form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No disputes for this client.');
    expect(fixture.nativeElement.textContent).not.toContain('No dispute feed connected');

    fixture.componentRef.setInput('client', { ...client, id: 43 });
    fixture.detectChanges();
    expect(service.search.calls.mostRecent().args[0].userId).toBe(43);
  });
});
