import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconComponent } from './icon';
import { IconName } from './icons';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="flex flex-col items-start gap-3">
      <span
        class="grid h-10 w-10 place-items-center rounded-xl bg-surface-overlay text-ink-muted ring-1 ring-line"
      >
        <app-icon [name]="icon()" [size]="18" />
      </span>
      <div class="space-y-1">
        <p class="text-sm font-medium text-ink">{{ title() }}</p>
        <p class="max-w-prose text-sm leading-relaxed text-ink-muted">{{ description() }}</p>
      </div>
      <ng-content />
    </div>
  `,
})
export class EmptyStateComponent {
  readonly icon = input<IconName>('help');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
}
