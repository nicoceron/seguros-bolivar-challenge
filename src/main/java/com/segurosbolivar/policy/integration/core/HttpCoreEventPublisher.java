package com.segurosbolivar.policy.integration.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpCoreEventPublisher implements CoreEventPublisher {

    private final RestClient restClient;

    public HttpCoreEventPublisher(
            RestClient.Builder restClientBuilder,
            @Value("${app.core.base-url}") String coreBaseUrl,
            @Value("${app.security.api-key}") String apiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(coreBaseUrl)
                .defaultHeader("x-api-key", apiKey)
                .build();
    }

    @Override
    public void publish(CoreEventPayload event) {
        restClient.post()
                .uri("/core-mock/evento")
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }
}

