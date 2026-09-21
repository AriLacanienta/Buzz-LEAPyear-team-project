import { Component } from '@angular/core';
import { SharedService } from '../../services/shared.service';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  accountName$ = this.sharedService.accountName$.pipe(
    map(name => name.split(' ')[0])
  );

  constructor(private sharedService: SharedService) { }
}
