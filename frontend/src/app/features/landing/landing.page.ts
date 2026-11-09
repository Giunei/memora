import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import gsap from 'gsap';
import { BrandComponent } from '../../shared/ui/brand';
import { IconComponent } from '../../shared/ui/icon';
import { IconName } from '../../shared/ui/icons';

interface Pillar {
  readonly icon: IconName;
  readonly title: string;
  readonly text: string;
}

@Component({
  selector: 'app-landing-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, BrandComponent, IconComponent],
  templateUrl: './landing.page.html',
})
export class LandingPage {
  private readonly host = inject(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly pillars: readonly Pillar[] = [
    {
      icon: 'brain',
      title: 'Active Recall',
      text: 'Você tenta lembrar antes de ver a resposta. É o treino que a memória realmente usa.',
    },
    {
      icon: 'repeat',
      title: 'Spaced Repetition',
      text: 'Revisões no momento certo, com FSRS — o agendador documentado, não um atalho.',
    },
    {
      icon: 'shuffle',
      title: 'Interleaving',
      text: 'Assuntos e formatos misturados na sessão, para o cérebro aprender a distinguir.',
    },
  ];

  readonly promises = [
    'Níveis CEFR de A1 a C2, por idioma',
    'Progresso, XP e sequência calculados no servidor',
    'Arquitetura pronta para concursos, programação e ciências',
  ] as const;

  readonly skills: readonly Pillar[] = [
    { icon: 'book', title: 'Vocabulário', text: 'Palavras, expressões e exemplos no contexto.' },
    { icon: 'pen', title: 'Gramática', text: 'Regras, aplicação e identificação de erros.' },
    { icon: 'globe', title: 'Leitura', text: 'Textos por nível e vocabulário desconhecido.' },
    { icon: 'headphones', title: 'Listening', text: 'Compreensão a partir de trechos de áudio.' },
    { icon: 'message', title: 'Escrita', text: 'Lacunas, tradução contextual e construção.' },
    { icon: 'sparkles', title: 'Conversação', text: 'Planejada; sem APIs pagas nesta etapa.' },
  ];

  readonly grades = ['Again', 'Hard', 'Good', 'Easy'] as const;

  constructor() {
    afterNextRender(() => {
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
      }
      const ctx = gsap.context(() => {
        gsap.from('.hero-anim', {
          y: 18,
          opacity: 0,
          duration: 0.7,
          stagger: 0.08,
          ease: 'power3.out',
        });
      }, this.host.nativeElement);
      this.destroyRef.onDestroy(() => ctx.revert());
    });
  }
}
