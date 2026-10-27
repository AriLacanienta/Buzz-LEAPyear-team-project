import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../services/auth.service";

@Component({
  selector: "app-login",
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.scss"],
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = ""; // For displaying error messages

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private authService: AuthService, // Inject AuthService
  ) {
    this.loginForm = this.formBuilder.group({
      email: ["", [Validators.required, Validators.email]],
      password: ["", [Validators.required, Validators.minLength(6)]],
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit() {
    this.submitted = true;
    this.errorMessage = ""; // Clear previous errors

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;

    //Get email and password from form
    const email = this.loginForm.get("email")?.value;
    const password = this.loginForm.get("password")?.value;

    //Call auth service login method
    //This makes HTTP request to backend /api/auth/login
    this.authService.login(email, password).subscribe(
      //success response
      (response) => {
        //storing access and refresh tokens from backend auth response
        this.authService.setAuthSessionFromResponse(response);

        this.loading = false;

        const landingPage = this.authService.hasRole('MANAGER')
          ? '/manager'
          : this.authService.hasRole('ANALYST')
            ? '/analyst'
            : '/dashboard';
        this.router.navigate([landingPage]);
      },
      //error response
      (error) => {
        this.loading = false;
        //checking if it's a technical/security error, shows generic message instead
        if (
          error.error?.message?.includes("signing key") ||
          error.error?.message?.includes("JWT")
        ) {
          this.errorMessage = "Login failed. Please try again.";
        } else {
          this.errorMessage =
            error.error?.message || "Login failed. Please try again.";
        }
        console.error("Login error:", error);
      },
    );
  }

  forgotPassword() {
    console.log("Forgot password clicked");
    // need to do:Navigate to forgot password page
  }
}
