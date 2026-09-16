import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { MarketsComponent } from './pages/markets/markets.component';

const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'portfolio', component: DashboardComponent },
  { path: 'markets', component: MarketsComponent }
];

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    MarketsComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    RouterModule.forRoot(routes)
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
