// Purpose of this file: Checks HTTP payloads and responses with a local test server.
package com.segurosbolivar.policy.integration.core;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class HttpCoreEventPublisherTest {
    HttpServer server;
    HttpCoreEventPublisher publisher;
    AtomicReference<String> body = new AtomicReference<>();
    AtomicReference<String> key = new AtomicReference<>();
    AtomicReference<String> idempotency = new AtomicReference<>();
    volatile int responseStatus = 202;

    @BeforeEach
    /** Prepares test data or the local test server before each case. */
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/core-mock/evento", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            idempotency.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        publisher = new HttpCoreEventPublisher(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "123456",
                Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @AfterEach
    /** Stops the local test server and releases resources. */
    void tearDown() { server.stop(0); }

    @Test
    /** Checks the required body and stable event ID over real HTTP. */
    void sendsRequiredPayloadAndStableIdOverRealHttp() {
        UUID id = UUID.randomUUID();
        publisher.publish(new CoreEventPayload("ACTUALIZACION", 555L, id));
        assertThat(body.get()).contains("\"evento\":\"ACTUALIZACION\"", "\"polizaId\":555", id.toString());
        assertThat(key.get()).isEqualTo("123456");
        assertThat(idempotency.get()).isEqualTo(id.toString());
    }

    @Test
    /** Checks an HTTP 5xx allows the outbox to retry. */
    void rejectsServerErrorsSoTheOutboxCanRetry() {
        responseStatus = 503;
        assertThatThrownBy(() -> publisher.publish(new CoreEventPayload("ACTUALIZACION", 555L, UUID.randomUUID())))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    /** Checks HTTP 302 is not treated as accepted delivery. */
    void redirectIsNotMistakenForAnAcknowledgement() {
        responseStatus = 302;
        assertThatThrownBy(() -> publisher.publish(new CoreEventPayload("ACTUALIZACION", 555L, UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class);
    }
}
