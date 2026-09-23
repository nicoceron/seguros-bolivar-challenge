// Purpose of this file: Sends an event to the CORE mock over HTTP.
package com.segurosbolivar.policy.integration.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class HttpCoreEventPublisher implements CoreEventPublisher {

    private final RestClient restClient;

    /** Sets the mock URL and HTTP connection and read timeouts. */
    public HttpCoreEventPublisher(
            RestClient.Builder restClientBuilder,
            @Value("${app.core.base-url}") String coreBaseUrl,
            @Value("${app.security.api-key}") String apiKey,
            @Value("${app.core.connect-timeout}") Duration connectTimeout,
            @Value("${app.core.read-timeout}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder
                .baseUrl(coreBaseUrl)
                .defaultHeader("x-api-key", apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    /** POSTs the event and treats only an HTTP 2xx response as success. */
    public void publish(CoreEventPayload event) {
        var response = restClient.post()
                .uri("/core-mock/evento")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", event.eventId().toString())
                .body(event)
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("CORE did not acknowledge the event: " + response.getStatusCode());
        }
    }
}
