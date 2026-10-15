import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ManagerTradeLogPage } from '../models/manager-trade-log.model';

@Injectable({ providedIn: 'root' })
export class ManagerTradeLogService {
    private apiUrl = `${environment.apiUrl}/manager/tradeorders`;

    constructor(private http: HttpClient) {}

    getTradeLog(page = 0, size = 25) {
        const params = new HttpParams()
        .set('page', page)
        .set('size', size);

        return this.http.get<ManagerTradeLogPage>(
            this.apiUrl,
            { params }
        );
    }
}