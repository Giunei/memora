import { describeApiError } from './api-error';

describe('describeApiError', () => {
  it('prefers field violations when they exist', () => {
    const message = describeApiError({
      timestamp: '',
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'ignored',
      path: '/api/auth/register',
      violations: [{ field: 'password', message: 'must contain a digit' }],
    });

    expect(message).toContain('password');
    expect(message).toContain('must contain a digit');
  });

  it('maps stable codes to a user-facing sentence', () => {
    expect(
      describeApiError({
        timestamp: '',
        status: 401,
        code: 'INVALID_CREDENTIALS',
        message: 'Invalid email or password',
        path: '/api/auth/login',
      }),
    ).toBe('E-mail ou senha incorretos.');
  });
});
