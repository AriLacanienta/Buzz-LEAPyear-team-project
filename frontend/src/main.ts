import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

console.log('=== Step 1: Main.ts loading ===');

platformBrowserDynamic()
  .bootstrapModule(AppModule)
  .then(() => {
    console.log('=== Step 2: Angular bootstrapped successfully ===');
  })
  .catch(err => {
    console.error('=== BOOTSTRAP ERROR ===', err);
    throw err;
  });

// platformBrowserDynamic().bootstrapModule(AppModule)
//   .catch(err => console.error(err));
