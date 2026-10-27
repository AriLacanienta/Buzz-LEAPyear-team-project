import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, Subject, tap } from "rxjs";
import { environment } from "../../environments/environment";
import {
  CreateDisputeRequest,
  DisputeResponse,
  DisputePage,
  DisputeSearch,
} from "../models/dispute.model";

@Injectable({ providedIn: "root" })
export class DisputeService {
  private readonly disputeCreatedSubject = new Subject<void>();
  readonly disputeCreated$ = this.disputeCreatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  create(request: CreateDisputeRequest): Observable<DisputeResponse> {
    return this.http.post<DisputeResponse>(
      `${environment.apiUrl}/dispute`,
      request,
    ).pipe(tap(() => this.disputeCreatedSubject.next()));
  }

  resolve(id: number, resolutionMessage: string): Observable<DisputeResponse> {
    return this.http.patch<DisputeResponse>(
      `${environment.apiUrl}/dispute/${id}/resolve`,
      { resolutionMessage },
    );
  }

  search(query: DisputeSearch): Observable<DisputePage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== "") {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<DisputePage>(`${environment.apiUrl}/dispute/search`, {
      params,
    });
  }
}
