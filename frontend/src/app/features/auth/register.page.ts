import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { describeApiError, toApiError } from '../../core/api/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { BrandComponent } from '../../shared/ui/brand';
import { IconComponent } from '../../shared/ui/icon';

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).+$/;

@Component({
  selector: 'app-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, IconComponent],
  templateUrl: './register.page.html',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(128),
        Validators.pattern(PASSWORD_PATTERN),
      ],
    ],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const { displayName, email, password } = this.form.getRawValue();
    this.auth
      .register({
        displayName,
        email,
        password,
        locale: navigator.language || 'pt-BR',
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
      .subscribe({
        next: () => void this.router.navigateByUrl('/app/dashboard'),
        error: (error: unknown) => {
          this.submitting.set(false);
          const apiError = toApiError(error);
          this.errorMessage.set(
            apiError.code === 'RESOURCE_ALREADY_EXISTS'
              ? 'Esse e-mail já está cadastrado. Tente entrar.'
              : describeApiError(apiError),
          );
        },
      });
  }
}
