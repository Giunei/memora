package com.memora.shared.error;

public class ResourceAlreadyExistsException extends ApplicationException {

    public ResourceAlreadyExistsException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }
}
