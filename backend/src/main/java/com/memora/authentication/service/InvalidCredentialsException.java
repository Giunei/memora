package com.memora.authentication.service;

import com.memora.shared.error.ApplicationException;
import com.memora.shared.error.ErrorCode;

/**
 * Thrown for both an unknown email and a wrong password. The message is
 * intentionally identical in either case so the endpoint cannot be used to
 * discover which addresses have accounts.
 */
public class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
    }
}
