import { Injectable } from "@angular/core";
import { environment } from "../../environments/environment";
import { Observable, Subject } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { OrderStatusResponse, TradeOrderPreviewRequest, TradeOrderPreviewResponse, TradeOrderRequest, TradeOrderResponse } from "@app/models/trade-order.model";

@Injectable({
    providedIn: 'root'
})
export class TradeOrderService {
    private apiUrl = `${environment.apiUrl}/tradeorders`;
    private orderPlacedSubject = new Subject<void>();
    orderPlaced$ = this.orderPlacedSubject.asObservable();

    constructor(private http: HttpClient) {}

    previewOrder(request: TradeOrderPreviewRequest): Observable<TradeOrderPreviewResponse> {
        return this.http.post<TradeOrderPreviewResponse>(`${this.apiUrl}/preview`, request);
    }

    submitOrder(request: TradeOrderRequest): Observable<TradeOrderResponse> {
        return this.http.post<TradeOrderResponse>(this.apiUrl, request);
    }

    getOrderStatus(orderId: number): Observable<OrderStatusResponse> {
        return this.http.get<OrderStatusResponse>(`${this.apiUrl}/${encodeURIComponent(orderId)}`);
    }

    notifyOrderplaced(): void {
        this.orderPlacedSubject.next();
    }
}