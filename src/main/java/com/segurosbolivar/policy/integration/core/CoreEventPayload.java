package com.segurosbolivar.policy.integration.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoreEventPayload(
        @JsonProperty("evento") String eventType,
        @JsonProperty("polizaId") Long policyId,
        @JsonProperty("eventId") UUID eventId
) {
}

