import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AiStatus } from './models';

@Injectable({ providedIn: 'root' })
export class AiApi {
  private readonly http = inject(HttpClient);

  status(): Observable<AiStatus> {
    return this.http.get<AiStatus>('/api/ai/status');
  }
}
