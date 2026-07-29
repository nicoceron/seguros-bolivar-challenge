package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.api.dto.CreatePolicyRequest;
import com.segurosbolivar.policy.api.dto.PageResponse;
import com.segurosbolivar.policy.api.dto.PolicyResponse;
import com.segurosbolivar.policy.api.dto.RenewPolicyRequest;
import com.segurosbolivar.policy.api.dto.RiskRequest;
import com.segurosbolivar.policy.api.dto.RiskResponse;
import com.segurosbolivar.policy.domain.PolicyStatus;
import com.segurosbolivar.policy.domain.PolicyType;
import com.segurosbolivar.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/polizas")
@Tag(name = "Polizas", description = "Rental policy lifecycle")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @Operation(summary = "List policies, optionally filtered by tipo and estado")
    public PageResponse<PolicyResponse> list(
            @RequestParam(required = false) PolicyType tipo,
            @RequestParam(required = false) PolicyStatus estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return policyService.findPolicies(tipo, estado, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve one policy")
    public PolicyResponse get(@PathVariable long id) {
        return policyService.getPolicy(id);
    }

    @PostMapping
    @Operation(summary = "Create a policy and its initial risk set")
    public ResponseEntity<PolicyResponse> create(
            @Valid @RequestBody CreatePolicyRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        PolicyResponse created = policyService.createPolicy(request);
        URI location = uriBuilder.path("/polizas/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}/riesgos")
    @Operation(summary = "List risks associated with a policy")
    public List<RiskResponse> risks(@PathVariable long id) {
        return policyService.getRisks(id);
    }

    @PostMapping("/{id}/renovar")
    @Operation(summary = "Renew a policy using an IPC percentage")
    public PolicyResponse renew(
            @PathVariable long id,
            @Valid @RequestBody RenewPolicyRequest request
    ) {
        return policyService.renew(id, request.ipcPercentage());
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancel a policy and all its risks")
    public PolicyResponse cancel(@PathVariable long id) {
        return policyService.cancelPolicy(id);
    }

    @PostMapping("/{id}/riesgos")
    @Operation(summary = "Add a risk to a collective policy")
    public ResponseEntity<RiskResponse> addRisk(
            @PathVariable long id,
            @Valid @RequestBody RiskRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        RiskResponse created = policyService.addRisk(id, request);
        URI location = uriBuilder.path("/riesgos/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}

