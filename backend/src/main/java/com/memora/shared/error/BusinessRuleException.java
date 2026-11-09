package com.memora.shared.error;

/**
 * A syntactically valid request that a domain rule refuses. Distinct from
 * {@code VALIDATION_FAILED}, which means the payload itself is malformed.
 */
public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
