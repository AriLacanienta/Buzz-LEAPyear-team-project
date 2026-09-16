import { Component } from '@angular/core';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss']
})
export class MarketsComponent {
  title = 'BuzzLEAPYear Trading Platform';

  constructor() {
    console.log('MarketsComponent initialized');
  }
}
