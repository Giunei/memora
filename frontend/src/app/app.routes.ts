import { Routes } from '@angular/router';
import { anonymousOnlyGuard, authenticatedGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    canActivate: [anonymousOnlyGuard],
    loadComponent: () => import('./features/landing/landing.page').then((m) => m.LandingPage),
  },
  {
    path: 'login',
    canActivate: [anonymousOnlyGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [anonymousOnlyGuard],
    loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'app',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./features/shell/app-shell').then((m) => m.AppShell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'languages',
        loadComponent: () =>
          import('./features/languages/languages.page').then((m) => m.LanguagesPage),
      },
      {
        path: 'languages/:enrolmentId',
        loadComponent: () =>
          import('./features/languages/language-detail.page').then((m) => m.LanguageDetailPage),
      },
      {
        path: 'study',
        loadComponent: () =>
          import('./features/coming-soon/coming-soon.page').then((m) => m.ComingSoonPage),
        data: {
          title: 'Sessão de estudo',
          icon: 'brain',
          description:
            'Aqui vai acontecer a recuperação ativa: você tenta lembrar antes de ver a resposta. O motor FSRS e os exercícios chegam na próxima etapa.',
        },
      },
      {
        path: 'stats',
        loadComponent: () =>
          import('./features/coming-soon/coming-soon.page').then((m) => m.ComingSoonPage),
        data: {
          title: 'Estatísticas',
          icon: 'bar-chart',
          description:
            'Retenção, taxa de acertos e evolução semanal vão nascer do histórico real de revisões — não de números inventados.',
        },
      },
      {
        path: 'achievements',
        loadComponent: () =>
          import('./features/coming-soon/coming-soon.page').then((m) => m.ComingSoonPage),
        data: {
          title: 'Conquistas',
          icon: 'trophy',
          description:
            'XP, níveis e sequência já existem na sua conta. Missões, medalhas e metas semanais entram junto com as sessões de estudo.',
        },
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.page').then((m) => m.ProfilePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
