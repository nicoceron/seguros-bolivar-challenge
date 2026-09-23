// Purpose of this file: Defines event delivery so tests can replace the HTTP implementation.
package com.segurosbolivar.policy.integration.core;

public interface CoreEventPublisher {

    /** Delivery contract: send the CORE event or throw so outbox can retry. */
    void publish(CoreEventPayload event);
}

