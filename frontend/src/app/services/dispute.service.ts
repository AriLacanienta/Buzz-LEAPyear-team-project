import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Dispute, DisputePage, DisputeSearch } from '../models/dispute.model';

@Injectable({ providedIn: 'root' })
export class DisputeService {
  constructor(private readonly http: HttpClient) {}

  resolve(id: number, resolutionMessage: string): Observable<Dispute> {
    return this.http.patch<Dispute>(`${environment.apiUrl}/dispute/${id}/resolve`, { resolutionMessage });
  }

  search(query: DisputeSearch): Observable<DisputePage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<DisputePage>(`${environment.apiUrl}/dispute/search`, { params });
  }
}
