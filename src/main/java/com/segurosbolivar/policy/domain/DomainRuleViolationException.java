// Purpose of this file: Represents a broken policy rule, which the API reports as HTTP 409.
package com.segurosbolivar.policy.domain;

public class DomainRuleViolationException extends RuntimeException {

    private final String code;

    /** Creates a business-rule error with a stable code and readable message. */
    public DomainRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Returns the stable error code used in the HTTP response. */
    public String getCode() {
        return code;
    }
}

