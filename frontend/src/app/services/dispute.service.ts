import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreateDisputeRequest, DisputeResponse } from '../models/dispute.model';

@Injectable({ providedIn: 'root' })
export class DisputeService {
  constructor(private http: HttpClient) {}

  create(request: CreateDisputeRequest): Observable<DisputeResponse> {
    return this.http.post<DisputeResponse>(`${environment.apiUrl}/dispute`, request);
  }
}