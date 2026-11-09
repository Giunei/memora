import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Progress } from './models';

@Injectable({ providedIn: 'root' })
export class ProgressApi {
  private readonly http = inject(HttpClient);

  load(): Observable<Progress> {
    return this.http.get<Progress>('/api/me/progress');
  }

  updateDailyGoal(dailyGoalMinutes: number): Observable<Progress> {
    return this.http.put<Progress>('/api/me/progress/daily-goal', { dailyGoalMinutes });
  }
}
