import { Injectable } from "@angular/core";
import { environment } from "../../environments/environment";
import { Subject } from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class TradeOrderService {
    private apiUrl = `${environment.apiUrl}/tradeorders`;
    private orderPlacedSubject = new Subject<void>();
    orderPlaced$ = this.orderPlacedSubject.asObservable();

    constructor() {}
}