// Purpose of this file: Receives HTTP requests to read or cancel one risk.
package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.api.dto.RiskResponse;
import com.segurosbolivar.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/riesgos")
@Tag(name = "Riesgos", description = "Insured rental risks")
public class RiskController {

    private final PolicyService policyService;

    /** Receives the service that can read and cancel risks. */
    public RiskController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/{id}")
    /** Returns one risk by ID. */
    public RiskResponse get(@PathVariable long id) {
        return policyService.getRisk(id);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancel one risk")
    /** Cancels one risk without cancelling its whole policy. */
    public RiskResponse cancel(@PathVariable long id) {
        return policyService.cancelRisk(id);
    }
}

