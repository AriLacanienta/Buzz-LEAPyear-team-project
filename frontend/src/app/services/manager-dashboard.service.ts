import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ManagerDashboardSummary } from '../models/manager-dashboard.model';

@Injectable({ providedIn: 'root' })
export class ManagerDashboardService {
  constructor(private readonly http: HttpClient) {}

  getSummary(): Observable<ManagerDashboardSummary> {
    return this.http.get<ManagerDashboardSummary>(`${environment.apiUrl}/dispute/summary`);
  }
}
