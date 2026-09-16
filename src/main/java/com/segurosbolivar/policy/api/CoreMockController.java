package com.segurosbolivar.policy.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/core-mock")
@Tag(name = "CORE mock", description = "Required legacy CORE delivery stub")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    @Operation(summary = "Log an attempted policy update delivery to CORE")
    public ResponseEntity<Void> event(@Valid @RequestBody CoreMockEventRequest request) {
        log.info(
                "CORE update delivery attempted event={} policyId={} eventId={}",
                request.event(),
                request.policyId(),
                request.eventId()
        );
        return ResponseEntity.accepted().build();
    }

    public record CoreMockEventRequest(
            @NotBlank @Pattern(regexp = "ACTUALIZACION") @JsonProperty("evento") String event,
            @NotNull @Positive @JsonProperty("polizaId") Long policyId,
            @JsonProperty("eventId") UUID eventId
    ) {
    }
}

