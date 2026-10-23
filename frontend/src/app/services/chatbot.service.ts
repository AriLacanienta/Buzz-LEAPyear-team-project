import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatRequest, ChatResponse } from '../models/chatbot.model';

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private readonly apiUrl = `${environment.apiUrl}/chatbot`;

  constructor(private http: HttpClient) {}

  sendMessage(message: string): Observable<ChatResponse> {
    const request: ChatRequest = { message };

    return this.http.post<ChatResponse>(this.apiUrl, request);
  }
}