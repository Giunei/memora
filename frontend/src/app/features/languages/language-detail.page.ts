import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { describeApiError, toApiError } from '../../core/api/api-error';
import { LanguagesApi } from '../../core/api/languages.api';
import { CEFR_LEVELS, CefrLevel, UserLanguage } from '../../core/api/models';
import { EmptyStateComponent } from '../../shared/ui/empty-state';
import { IconComponent } from '../../shared/ui/icon';

@Component({
  selector: 'app-language-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, EmptyStateComponent, IconComponent],
  templateUrl: './language-detail.page.html',
})
export class LanguageDetailPage {
  private readonly languagesApi = inject(LanguagesApi);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly enrolmentId = input.required<string>();
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly removing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly enrolment = signal<UserLanguage | null>(null);
  readonly cefrLevels = CEFR_LEVELS;

  readonly form = this.formBuilder.nonNullable.group({
    currentLevel: this.formBuilder.nonNullable.control<CefrLevel>('A1'),
    targetLevel: this.formBuilder.nonNullable.control<CefrLevel>('B2'),
    primaryFocus: [false],
  });

  constructor() {
    toObservable(this.enrolmentId)
      .pipe(takeUntilDestroyed())
      .subscribe((id) => this.load(id));
  }

  save(): void {
    if (this.saving()) {
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    this.languagesApi.update(this.enrolmentId(), this.form.getRawValue()).subscribe({
      next: (enrolment) => {
        this.enrolment.set(enrolment);
        this.saving.set(false);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.errorMessage.set(describeApiError(toApiError(error)));
      },
    });
  }

  remove(): void {
    if (
      this.removing() ||
      !confirm('Parar de estudar este idioma? O progresso de revisão ainda não existe nesta etapa.')
    ) {
      return;
    }
    this.removing.set(true);
    this.languagesApi.remove(this.enrolmentId()).subscribe({
      next: () => void this.router.navigateByUrl('/app/languages'),
      error: (error: unknown) => {
        this.removing.set(false);
        this.errorMessage.set(describeApiError(toApiError(error)));
      },
    });
  }

  private load(id: string): void {
    this.loading.set(true);
    this.languagesApi.getMine(id).subscribe({
      next: (enrolment) => {
        this.enrolment.set(enrolment);
        this.form.reset({
          currentLevel: enrolment.currentLevel,
          targetLevel: enrolment.targetLevel,
          primaryFocus: enrolment.primaryFocus,
        });
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(describeApiError(toApiError(error)));
        this.loading.set(false);
      },
    });
  }
}
