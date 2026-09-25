import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.registerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get f() {
    return this.registerForm.controls;
  }

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';

    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;

    const { username, email, firstName, lastName, password } = this.registerForm.value;

    this.authService.register(username, email, firstName, lastName, password).subscribe(
      (response) => {
        this.authService.storeToken(response.token);
        this.loading = false;
        console.log('Registration successful!', response);
        this.router.navigate(['/dashboard']);
      },
      (error) => {
        this.loading = false;
        // Check if it's a technical/security error, show generic message instead
        if (error.error?.message?.includes('signing key') || error.error?.message?.includes('JWT')) {
          this.errorMessage = 'Please enter a strong password.';
        } else {
          this.errorMessage = error.error?.message || 'Please enter a strong password.';
        }
        console.error('Registration error:', error);
      }
    );
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
