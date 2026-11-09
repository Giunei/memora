import { HttpErrorResponse } from '@angular/common/http';

/** Mirrors the backend's `ApiError` contract. */
export interface ApiError {
  readonly timestamp: string;
  readonly status: number;
  readonly code: ApiErrorCode;
  readonly message: string;
  readonly path: string;
  readonly violations?: readonly FieldViolation[];
}

export interface FieldViolation {
  readonly field: string;
  readonly message: string;
}

export type ApiErrorCode =
  | 'VALIDATION_FAILED'
  | 'MALFORMED_REQUEST'
  | 'AUTHENTICATION_FAILED'
  | 'INVALID_CREDENTIALS'
  | 'INVALID_REFRESH_TOKEN'
  | 'ACCOUNT_NOT_ACTIVE'
  | 'ACCESS_DENIED'
  | 'RESOURCE_NOT_FOUND'
  | 'RESOURCE_ALREADY_EXISTS'
  | 'BUSINESS_RULE_VIOLATION'
  | 'RATE_LIMIT_EXCEEDED'
  | 'AI_UNAVAILABLE'
  | 'INTERNAL_ERROR'
  | 'NETWORK_ERROR';

const MESSAGES: Partial<Record<ApiErrorCode, string>> = {
  INVALID_CREDENTIALS: 'E-mail ou senha incorretos.',
  RESOURCE_ALREADY_EXISTS: 'Esse registro já existe.',
  AUTHENTICATION_FAILED: 'Sua sessão expirou. Entre novamente.',
  INVALID_REFRESH_TOKEN: 'Sua sessão expirou. Entre novamente.',
  ACCOUNT_NOT_ACTIVE: 'Esta conta não está ativa.',
  ACCESS_DENIED: 'Você não tem permissão para essa ação.',
  RESOURCE_NOT_FOUND: 'Não encontramos o que você procura.',
  BUSINESS_RULE_VIOLATION: 'Essa ação não pode ser concluída.',
  VALIDATION_FAILED: 'Confira os campos destacados e tente de novo.',
  MALFORMED_REQUEST: 'A requisição está incompleta ou em um formato inválido.',
  RATE_LIMIT_EXCEEDED: 'Muitas requisições. Tente de novo em instantes.',
  AI_UNAVAILABLE: 'Os recursos de IA estão indisponíveis agora.',
  INTERNAL_ERROR: 'Algo deu errado do nosso lado. Já estamos sabendo.',
  NETWORK_ERROR: 'Não foi possível falar com o servidor. Verifique sua conexão.',
};

function isApiError(body: unknown): body is ApiError {
  return typeof body === 'object' && body !== null && 'code' in body && 'message' in body;
}

/**
 * Narrows an `HttpErrorResponse` to the backend contract when possible.
 *
 * A status of 0 means the request never reached the server, which is a
 * different problem from a server-side rejection and deserves its own code.
 */
export function toApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    if (isApiError(error.error)) {
      return error.error;
    }
    return {
      timestamp: new Date().toISOString(),
      status: error.status,
      code: error.status === 0 ? 'NETWORK_ERROR' : 'INTERNAL_ERROR',
      message: error.message,
      path: error.url ?? '',
    };
  }
  return {
    timestamp: new Date().toISOString(),
    status: 0,
    code: 'INTERNAL_ERROR',
    message: String(error),
    path: '',
  };
}

/**
 * A message safe to show a user.
 *
 * Field-level violations are surfaced verbatim because they name the offending
 * input; everything else falls back to a translated, non-technical sentence so
 * raw server text never reaches the interface.
 */
export function describeApiError(error: ApiError): string {
  if (error.violations?.length) {
    return error.violations
      .map((violation) => `${violation.field}: ${violation.message}`)
      .join(' · ');
  }
  return MESSAGES[error.code] ?? error.message ?? 'Erro inesperado.';
}
