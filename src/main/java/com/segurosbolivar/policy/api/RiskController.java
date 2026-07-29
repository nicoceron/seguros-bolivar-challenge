package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.api.dto.RiskResponse;
import com.segurosbolivar.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/riesgos")
@Tag(name = "Riesgos", description = "Insured rental risks")
public class RiskController {

    private final PolicyService policyService;

    public RiskController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancel one risk")
    public RiskResponse cancel(@PathVariable long id) {
        return policyService.cancelRisk(id);
    }
}

