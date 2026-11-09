import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { BrandComponent } from '../../shared/ui/brand';
import { IconComponent } from '../../shared/ui/icon';
import { IconName } from '../../shared/ui/icons';

interface NavItem {
  readonly label: string;
  readonly path: string;
  readonly icon: IconName;
  readonly mobile?: boolean;
}

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BrandComponent, IconComponent],
  templateUrl: './app-shell.html',
})
export class AppShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly drawerOpen = signal(false);
  readonly user = this.auth.user;
  readonly initials = computed(() => {
    const name = this.user()?.displayName ?? '';
    return name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase();
  });

  readonly nav: readonly NavItem[] = [
    { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard', mobile: true },
    { label: 'Idiomas', path: '/app/languages', icon: 'languages', mobile: true },
    { label: 'Estudar', path: '/app/study', icon: 'brain', mobile: true },
    { label: 'Estatísticas', path: '/app/stats', icon: 'bar-chart' },
    { label: 'Conquistas', path: '/app/achievements', icon: 'trophy' },
    { label: 'Perfil', path: '/app/profile', icon: 'user', mobile: true },
  ];

  readonly mobileNav = this.nav.filter((item) => item.mobile);

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: () => void this.router.navigateByUrl('/'),
    });
  }
}
