import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardSnapshot } from './models';

@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  load(): Observable<DashboardSnapshot> {
    return this.http.get<DashboardSnapshot>('/api/dashboard');
  }
}
