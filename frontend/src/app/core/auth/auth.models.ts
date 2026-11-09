export type Role = 'USER' | 'ADMIN';

export interface AuthUser {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly locale: string;
  readonly timeZone: string;
  readonly roles: readonly Role[];
  readonly createdAt: string;
}

export interface AuthResponse {
  readonly accessToken: string;
  readonly tokenType: string;
  readonly expiresIn: number;
  readonly user: AuthUser;
}

export interface RegisterPayload {
  readonly email: string;
  readonly password: string;
  readonly displayName: string;
  readonly locale?: string;
  readonly timeZone?: string;
}

export interface LoginPayload {
  readonly email: string;
  readonly password: string;
}
