import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NavbarComponent } from '@app/components/navbar/navbar.component';
import { InstrumentService } from '@app/services/instrument.service';

@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss']
})
export class MarketsComponent implements OnInit {
  title = 'BuzzLEAPYear Trading Platform';
  instruments: any[] = [];

  constructor(private instrumentService: InstrumentService) {
    console.log('MarketsComponent initialized');
  }

  // fetch data after component initialization
  ngOnInit(): void {
    this.instrumentService.getInstruments().subscribe({
      next: (response: any) => {
        this.instruments = response.content;
        console.log('Instruments loaded:', this.instruments);
      },
      error: (error) => {
        console.error('Error loading instruments:', error);
      }
    });
  }
}
