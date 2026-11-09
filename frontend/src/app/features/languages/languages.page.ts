import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { describeApiError, toApiError } from '../../core/api/api-error';
import { LanguagesApi } from '../../core/api/languages.api';
import { CEFR_LEVELS, CefrLevel, Language, UserLanguage } from '../../core/api/models';
import { EmptyStateComponent } from '../../shared/ui/empty-state';
import { IconComponent } from '../../shared/ui/icon';

@Component({
  selector: 'app-languages-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, EmptyStateComponent, IconComponent],
  templateUrl: './languages.page.html',
})
export class LanguagesPage {
  private readonly languagesApi = inject(LanguagesApi);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly enrolOpen = signal(false);
  readonly enrolments = signal<UserLanguage[]>([]);
  readonly catalogue = signal<Language[]>([]);
  readonly cefrLevels = CEFR_LEVELS;

  readonly availableLanguages = computed(() => {
    const taken = new Set(this.enrolments().map((item) => item.language.code));
    return this.catalogue().filter((language) => !taken.has(language.code));
  });

  readonly form = this.formBuilder.nonNullable.group({
    languageCode: ['', Validators.required],
    currentLevel: this.formBuilder.nonNullable.control<CefrLevel>('A1'),
    targetLevel: this.formBuilder.nonNullable.control<CefrLevel>('B2'),
    primaryFocus: [false],
  });

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      mine: this.languagesApi.listMine(),
      catalogue: this.languagesApi.listCatalogue(),
    }).subscribe({
      next: ({ mine, catalogue }) => {
        this.enrolments.set(mine);
        this.catalogue.set([...catalogue.content]);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(describeApiError(toApiError(error)));
        this.loading.set(false);
      },
    });
  }

  openEnrol(): void {
    const first = this.availableLanguages()[0];
    this.form.reset({
      languageCode: first?.code ?? '',
      currentLevel: 'A1',
      targetLevel: 'B2',
      primaryFocus: this.enrolments().length === 0,
    });
    this.enrolOpen.set(true);
  }

  enrol(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.languagesApi.enrol(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving.set(false);
        this.enrolOpen.set(false);
        this.refresh();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.errorMessage.set(describeApiError(toApiError(error)));
      },
    });
  }
}
