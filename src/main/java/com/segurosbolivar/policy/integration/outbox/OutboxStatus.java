// Purpose of this file: Lists pending, sent, and permanently failed delivery states.
package com.segurosbolivar.policy.integration.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}

