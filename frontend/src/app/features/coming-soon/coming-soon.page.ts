import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { IconComponent } from '../../shared/ui/icon';
import { IconName } from '../../shared/ui/icons';

@Component({
  selector: 'app-coming-soon-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent, RouterLink],
  template: `
    <section class="mx-auto max-w-xl animate-rise">
      <span
        class="grid h-12 w-12 place-items-center rounded-2xl bg-violet/15 text-violet-bright ring-1 ring-violet/30"
      >
        <app-icon [name]="icon()" [size]="22" />
      </span>
      <h1 class="mt-6 font-display text-3xl font-semibold tracking-tight">{{ title() }}</h1>
      <p class="mt-3 text-ink-muted">{{ description() }}</p>
      <a routerLink="/app/dashboard" class="btn-secondary mt-8">
        <app-icon name="arrow-left" [size]="16" />
        Voltar ao dashboard
      </a>
    </section>
  `,
})
export class ComingSoonPage {
  private readonly data = toSignal(
    inject(ActivatedRoute).data.pipe(
      map((value) => ({
        title: String(value['title'] ?? 'Em breve'),
        description: String(value['description'] ?? ''),
        icon: (value['icon'] as IconName) ?? 'sparkles',
      })),
    ),
    {
      initialValue: {
        title: 'Em breve',
        description: '',
        icon: 'sparkles' as IconName,
      },
    },
  );

  readonly title = computed(() => this.data().title);
  readonly description = computed(() => this.data().description);
  readonly icon = computed(() => this.data().icon);
}
