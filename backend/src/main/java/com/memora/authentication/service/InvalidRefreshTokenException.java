package com.memora.authentication.service;

import com.memora.shared.error.ApplicationException;
import com.memora.shared.error.ErrorCode;

public class InvalidRefreshTokenException extends ApplicationException {

    public InvalidRefreshTokenException(String message) {
        super(ErrorCode.INVALID_REFRESH_TOKEN, message);
    }
}
