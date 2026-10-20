import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ManagerTradeDetail, ManagerTradeLogPage } from '../models/manager-trade-log.model';

@Injectable({ providedIn: 'root' })
export class ManagerTradeLogService {
    private apiUrl = `${environment.apiUrl}/manager/tradeorders`;

    constructor(private http: HttpClient) {}

    getTradeLog(page = 0, size = 25): Observable<ManagerTradeLogPage> {
        const params = new HttpParams()
            .set('page', String(page))
            .set('size', String(size));

        return this.http.get<ManagerTradeLogPage>(this.apiUrl, { params });
    }

    getTradeDetail(tradeId: number): Observable<ManagerTradeDetail> {
        return this.http.get<ManagerTradeDetail>(`${this.apiUrl}/${tradeId}`);
    }
}
