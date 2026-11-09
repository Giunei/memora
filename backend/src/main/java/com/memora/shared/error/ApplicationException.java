package com.memora.shared.error;

import lombok.Getter;

/**
 * Base class for failures that are part of the API contract and therefore map to
 * a deliberate response, as opposed to bugs, which surface as 500.
 */
@Getter
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode code;

    protected ApplicationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    protected ApplicationException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
