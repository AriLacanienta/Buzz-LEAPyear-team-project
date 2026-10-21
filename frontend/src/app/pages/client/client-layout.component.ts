import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../components/client/navbar/navbar.component';
import { ChatbotComponent } from '../../components/client/chatbot/chatbot.component';

@Component({
  selector: 'app-client-layout',
  standalone: true,
  imports: [NavbarComponent, RouterOutlet, ChatbotComponent],
  templateUrl: './client-layout.component.html',
  styleUrls: ['./client-layout.component.scss']
})
export class ClientLayoutComponent {}
