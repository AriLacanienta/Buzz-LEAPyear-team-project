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
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).+$/)
      ]]
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
        this.authService.setAuthSessionFromResponse(response);
        this.loading = false;
        console.log('Registration successful!', response);

        const landingPage = this.authService.hasRole('MANAGER')
          ? '/admin'
          : this.authService.hasRole('ANALYST')
            ? '/analyst'
            : '/dashboard';
        this.router.navigate([landingPage]);
      },
      (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message ||
          'Registration failed. Please check your details and try again.';
        console.error('Registration error:', error);
      }
    );
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
