import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { describeApiError, toApiError } from '../../core/api/api-error';
import { ProgressApi } from '../../core/api/progress.api';
import { UsersApi } from '../../core/api/users.api';
import { AuthService } from '../../core/auth/auth.service';
import { IconComponent } from '../../shared/ui/icon';

@Component({
  selector: 'app-profile-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent],
  templateUrl: './profile.page.html',
})
export class ProfilePage {
  private readonly usersApi = inject(UsersApi);
  private readonly progressApi = inject(ProgressApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly user = this.auth.user;
  readonly savingProfile = signal(false);
  readonly savingGoal = signal(false);
  readonly profileMessage = signal<string | null>(null);
  readonly goalMessage = signal<string | null>(null);
  readonly profileError = signal<string | null>(null);
  readonly goalError = signal<string | null>(null);

  readonly profileForm = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    locale: ['pt-BR', [Validators.required, Validators.maxLength(16)]],
    timeZone: ['UTC', [Validators.required, Validators.maxLength(64)]],
  });

  readonly goalForm = this.formBuilder.nonNullable.group({
    dailyGoalMinutes: [15, [Validators.required, Validators.min(5), Validators.max(480)]],
  });

  constructor() {
    const user = this.auth.user();
    if (user) {
      this.profileForm.reset({
        displayName: user.displayName,
        locale: user.locale,
        timeZone: user.timeZone,
      });
    }
    this.progressApi.load().subscribe({
      next: (progress) => this.goalForm.reset({ dailyGoalMinutes: progress.dailyGoalMinutes }),
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.savingProfile()) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.savingProfile.set(true);
    this.profileError.set(null);
    this.profileMessage.set(null);
    this.usersApi.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: (user) => {
        this.auth.patchUser(user);
        this.savingProfile.set(false);
        this.profileMessage.set('Perfil atualizado.');
      },
      error: (error: unknown) => {
        this.savingProfile.set(false);
        this.profileError.set(describeApiError(toApiError(error)));
      },
    });
  }

  saveGoal(): void {
    if (this.goalForm.invalid || this.savingGoal()) {
      this.goalForm.markAllAsTouched();
      return;
    }
    this.savingGoal.set(true);
    this.goalError.set(null);
    this.goalMessage.set(null);
    this.progressApi.updateDailyGoal(this.goalForm.getRawValue().dailyGoalMinutes).subscribe({
      next: () => {
        this.savingGoal.set(false);
        this.goalMessage.set('Meta diária atualizada.');
      },
      error: (error: unknown) => {
        this.savingGoal.set(false);
        this.goalError.set(describeApiError(toApiError(error)));
      },
    });
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: () => void this.router.navigateByUrl('/'),
    });
  }
}
