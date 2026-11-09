package com.memora.authentication.service;

import com.memora.shared.error.ApplicationException;
import com.memora.shared.error.ErrorCode;

public class AccountNotActiveException extends ApplicationException {

    public AccountNotActiveException() {
        super(ErrorCode.ACCOUNT_NOT_ACTIVE, "This account is not active");
    }
}
