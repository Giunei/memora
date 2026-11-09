import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthUser } from '../auth/auth.models';
import { UpdateProfilePayload } from './models';

@Injectable({ providedIn: 'root' })
export class UsersApi {
  private readonly http = inject(HttpClient);

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>('/api/users/me');
  }

  updateProfile(payload: UpdateProfilePayload): Observable<AuthUser> {
    return this.http.put<AuthUser>('/api/users/me', payload);
  }
}
