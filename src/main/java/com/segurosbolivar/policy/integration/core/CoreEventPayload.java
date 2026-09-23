// Purpose of this file: Defines the JSON sent by the worker to the CORE mock.
package com.segurosbolivar.policy.integration.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
/** Event type, policy ID, and stable event ID sent to the CORE mock. */
public record CoreEventPayload(
        @JsonProperty("evento") String eventType,
        @JsonProperty("polizaId") Long policyId,
        @JsonProperty("eventId") UUID eventId
) {
}

