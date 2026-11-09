import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EnrolLanguagePayload,
  Language,
  PageResponse,
  UpdateUserLanguagePayload,
  UserLanguage,
} from './models';

@Injectable({ providedIn: 'root' })
export class LanguagesApi {
  private readonly http = inject(HttpClient);

  listCatalogue(search = '', size = 50): Observable<PageResponse<Language>> {
    return this.http.get<PageResponse<Language>>('/api/languages', {
      params: { search, page: 0, size },
    });
  }

  listMine(): Observable<UserLanguage[]> {
    return this.http.get<UserLanguage[]>('/api/me/languages');
  }

  getMine(enrolmentId: string): Observable<UserLanguage> {
    return this.http.get<UserLanguage>(`/api/me/languages/${enrolmentId}`);
  }

  enrol(payload: EnrolLanguagePayload): Observable<UserLanguage> {
    return this.http.post<UserLanguage>('/api/me/languages', payload);
  }

  update(enrolmentId: string, payload: UpdateUserLanguagePayload): Observable<UserLanguage> {
    return this.http.put<UserLanguage>(`/api/me/languages/${enrolmentId}`, payload);
  }

  remove(enrolmentId: string): Observable<void> {
    return this.http.delete<void>(`/api/me/languages/${enrolmentId}`);
  }
}
