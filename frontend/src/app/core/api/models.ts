export type CefrLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';

export const CEFR_LEVELS: readonly CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

export interface PageResponse<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly first: boolean;
  readonly last: boolean;
}

export interface Language {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly nativeName: string;
  readonly flagEmoji: string;
}

export interface UserLanguage {
  readonly id: string;
  readonly language: Language;
  readonly currentLevel: CefrLevel;
  readonly targetLevel: CefrLevel;
  readonly primaryFocus: boolean;
  readonly startedAt: string;
  readonly lastStudiedAt?: string;
}

export interface Progress {
  readonly totalXp: number;
  readonly level: number;
  readonly xpIntoLevel: number;
  readonly xpForNextLevel: number;
  readonly currentStreakDays: number;
  readonly longestStreakDays: number;
  readonly lastStudyDate?: string;
  readonly totalStudyMinutes: number;
  readonly dailyGoalMinutes: number;
}

export interface DashboardSnapshot {
  readonly progress: Progress;
  readonly languages: readonly UserLanguage[];
}

export interface AiStatus {
  readonly available: boolean;
  readonly provider: string;
  readonly model: string;
  readonly dailyRequestLimit: number;
}

export interface EnrolLanguagePayload {
  readonly languageCode: string;
  readonly currentLevel: CefrLevel;
  readonly targetLevel: CefrLevel;
  readonly primaryFocus: boolean;
}

export interface UpdateUserLanguagePayload {
  readonly currentLevel: CefrLevel;
  readonly targetLevel: CefrLevel;
  readonly primaryFocus: boolean;
}

export interface UpdateProfilePayload {
  readonly displayName: string;
  readonly locale: string;
  readonly timeZone: string;
}
