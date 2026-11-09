import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import { forkJoin } from 'rxjs';
import { AiApi } from '../../core/api/ai.api';
import { describeApiError, toApiError } from '../../core/api/api-error';
import { DashboardApi } from '../../core/api/dashboard.api';
import { AiStatus, DashboardSnapshot } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../shared/ui/empty-state';
import { IconComponent } from '../../shared/ui/icon';
import { IconName } from '../../shared/ui/icons';

interface StatCard {
  readonly icon: IconName;
  readonly label: string;
  readonly value: string;
  readonly hint: string;
}

@Component({
  selector: 'app-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, NgxEchartsDirective, EmptyStateComponent, IconComponent],
  providers: [provideEchartsCore({ echarts: () => import('echarts') })],
  templateUrl: './dashboard.page.html',
})
export class DashboardPage {
  private readonly dashboardApi = inject(DashboardApi);
  private readonly aiApi = inject(AiApi);
  private readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly snapshot = signal<DashboardSnapshot | null>(null);
  readonly aiStatus = signal<AiStatus | null>(null);

  readonly user = this.auth.user;
  readonly progress = computed(() => this.snapshot()?.progress ?? null);
  readonly languages = computed(() => this.snapshot()?.languages ?? []);
  readonly greeting = computed(() => {
    const hours = new Date().getHours();
    if (hours < 12) {
      return 'Bom dia';
    }
    if (hours < 18) {
      return 'Boa tarde';
    }
    return 'Boa noite';
  });

  readonly xpPercent = computed(() => {
    const progress = this.progress();
    if (!progress || progress.xpForNextLevel === 0) {
      return 0;
    }
    return Math.min(100, Math.round((progress.xpIntoLevel / progress.xpForNextLevel) * 100));
  });

  readonly stats = computed<StatCard[]>(() => {
    const progress = this.progress();
    if (!progress) {
      return [];
    }
    return [
      {
        icon: 'zap',
        label: 'Nível',
        value: String(progress.level),
        hint: `${progress.totalXp} XP no total`,
      },
      {
        icon: 'flame',
        label: 'Sequência',
        value: `${progress.currentStreakDays}d`,
        hint: `Recorde: ${progress.longestStreakDays} dias`,
      },
      {
        icon: 'clock',
        label: 'Tempo de estudo',
        value: formatMinutes(progress.totalStudyMinutes),
        hint: 'Acumulado na conta',
      },
      {
        icon: 'target',
        label: 'Meta diária',
        value: `${progress.dailyGoalMinutes} min`,
        hint: 'Definida por você',
      },
    ];
  });

  readonly languageChart = computed<EChartsCoreOption | null>(() => {
    const languages = this.languages();
    if (languages.length === 0) {
      return null;
    }
    return {
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item' },
      legend: {
        bottom: 0,
        textStyle: { color: '#a3adc9', fontFamily: 'Inter' },
      },
      series: [
        {
          type: 'pie',
          radius: ['48%', '72%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#0b0f1b', borderWidth: 3 },
          label: { color: '#eef1fa', fontFamily: 'Inter', formatter: '{b}' },
          data: languages.map((enrolment) => ({
            name: enrolment.language.name,
            value: 1,
          })),
          color: ['#8b5cf6', '#3b82f6', '#22d3ee', '#34d399', '#fbbf24', '#fb7185'],
        },
      ],
    };
  });

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      dashboard: this.dashboardApi.load(),
      ai: this.aiApi.status(),
    }).subscribe({
      next: ({ dashboard, ai }) => {
        this.snapshot.set(dashboard);
        this.aiStatus.set(ai);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(describeApiError(toApiError(error)));
        this.loading.set(false);
      },
    });
  }
}

function formatMinutes(total: number): string {
  if (total < 60) {
    return `${total} min`;
  }
  const hours = Math.floor(total / 60);
  const minutes = total % 60;
  return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`;
}
