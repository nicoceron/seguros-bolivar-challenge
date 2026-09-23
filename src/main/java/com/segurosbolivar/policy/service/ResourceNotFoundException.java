// Purpose of this file: Represents a missing ID, which the API reports as HTTP 404.
package com.segurosbolivar.policy.service;

public class ResourceNotFoundException extends RuntimeException {

    private final String code;

    /** Creates a missing-resource error with code and message. */
    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Returns the stable error code used in the HTTP response. */
    public String getCode() {
        return code;
    }
}

