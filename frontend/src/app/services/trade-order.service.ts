import { Injectable } from "@angular/core";
import { environment } from "../../environments/environment";
import { Observable, Subject } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { OrderStatus, OrderStatusResponse, TradeOrderPreviewRequest, TradeOrderPreviewResponse, TradeOrderRequest, TradeOrderResponse } from "@app/models/trade-order.model";

@Injectable({
    providedIn: 'root'
})
export class TradeOrderService {
    private apiUrl = `${environment.apiUrl}/tradeorders`;
    private orderPlacedSubject = new Subject<void>();
    private orderCompletedSubject = new Subject<OrderStatus>();
    orderPlaced$ = this.orderPlacedSubject.asObservable();
    orderCompleted$ = this.orderCompletedSubject.asObservable();

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

    notifyOrderCompleted(status: OrderStatus): void {
        this.orderCompletedSubject.next(status);
    }
}