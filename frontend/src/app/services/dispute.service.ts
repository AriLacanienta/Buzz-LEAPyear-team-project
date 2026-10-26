import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";
import {
  CreateDisputeRequest,
  DisputeResponse,
  DisputePage,
  DisputeSearch,
} from "../models/dispute.model";

@Injectable({ providedIn: "root" })
export class DisputeService {
  constructor(private http: HttpClient) {}

  create(request: CreateDisputeRequest): Observable<DisputeResponse> {
    return this.http.post<DisputeResponse>(
      `${environment.apiUrl}/dispute`,
      request,
    );
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
