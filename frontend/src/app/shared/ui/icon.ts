import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  inject,
  input,
} from '@angular/core';
import { createElement } from 'lucide';
import { APP_ICONS, IconName } from './icons';

/**
 * Renders a Lucide glyph as an inline SVG.
 *
 * Built imperatively because Lucide ships icons as data (tag plus attribute
 * tuples) rather than markup, and an Angular template cannot pick an SVG
 * element name dynamically. The effect re-renders only when an input changes.
 */
@Component({
  selector: 'app-icon',
  standalone: true,
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'inline-flex shrink-0 items-center justify-center',
    'aria-hidden': 'true',
  },
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input(20);
  readonly strokeWidth = input(1.75);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    effect(() => {
      const svg = createElement(APP_ICONS[this.name()]);
      svg.setAttribute('width', String(this.size()));
      svg.setAttribute('height', String(this.size()));
      svg.setAttribute('stroke-width', String(this.strokeWidth()));
      svg.setAttribute('focusable', 'false');
      this.host.nativeElement.replaceChildren(svg);
    });
  }
}
