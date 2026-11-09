import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-brand',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <a [routerLink]="to()" class="group inline-flex items-center gap-2.5">
      <span
        class="grid h-9 w-9 place-items-center rounded-xl bg-violet/15 ring-1 ring-violet/35 transition group-hover:bg-violet/25"
      >
        <svg viewBox="0 0 32 32" class="h-5 w-5" aria-hidden="true">
          <path
            d="M6 24V9.2c0-.9.7-1.5 1.5-1.2l7.3 3.1c.8.3 1.6.3 2.4 0l7.3-3.1c.8-.3 1.5.3 1.5 1.2V24"
            fill="none"
            stroke="#a78bfa"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M16 11.5V24"
            fill="none"
            stroke="#60a5fa"
            stroke-width="2"
            stroke-linecap="round"
          />
        </svg>
      </span>
      @if (showWordmark()) {
        <span class="font-display text-[1.05rem] font-semibold tracking-tight text-ink">
          Memora
        </span>
      }
    </a>
  `,
})
export class BrandComponent {
  readonly to = input('/');
  readonly showWordmark = input(true);
}
