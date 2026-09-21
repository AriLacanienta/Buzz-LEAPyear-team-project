import { Component, OnInit } from '@angular/core';
import { SharedService } from './services/shared.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  constructor(private sharedService: SharedService) {
    console.log('AppComponent initialized');
  }

  ngOnInit() {
    this.sharedService.setAccountName('Joanna Smith');
  }
}
