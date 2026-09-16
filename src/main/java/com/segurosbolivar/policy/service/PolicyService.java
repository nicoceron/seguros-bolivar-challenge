package com.segurosbolivar.policy.service;

import com.segurosbolivar.policy.api.dto.CreatePolicyRequest;
import com.segurosbolivar.policy.api.dto.PageResponse;
import com.segurosbolivar.policy.api.dto.PolicyResponse;
import com.segurosbolivar.policy.api.dto.RiskRequest;
import com.segurosbolivar.policy.api.dto.RiskResponse;
import com.segurosbolivar.policy.domain.DomainRuleViolationException;
import com.segurosbolivar.policy.domain.Policy;
import com.segurosbolivar.policy.domain.PolicyStatus;
import com.segurosbolivar.policy.domain.PolicyType;
import com.segurosbolivar.policy.domain.Risk;
import com.segurosbolivar.policy.domain.RiskStatus;
import com.segurosbolivar.policy.integration.outbox.CoreOutboxEvent;
import com.segurosbolivar.policy.repository.CoreOutboxEventRepository;
import com.segurosbolivar.policy.repository.PolicyRepository;
import com.segurosbolivar.policy.repository.RiskRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final RiskRepository riskRepository;
    private final CoreOutboxEventRepository outboxRepository;

    public PolicyService(
            PolicyRepository policyRepository,
            RiskRepository riskRepository,
            CoreOutboxEventRepository outboxRepository
    ) {
        this.policyRepository = policyRepository;
        this.riskRepository = riskRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> findPolicies(
            PolicyType type,
            PolicyStatus status,
            Pageable pageable
    ) {
        Page<Policy> policies = policyRepository.findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable);
        return PageResponse.from(policies, PolicyResponse::from);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(long policyId) {
        return PolicyResponse.from(requirePolicy(policyId));
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> getRisks(long policyId) {
        requirePolicy(policyId);
        return riskRepository.findByPolicyIdOrderByIdAsc(policyId)
                .stream()
                .map(RiskResponse::from)
                .toList();
    }

    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        if (request.type() == PolicyType.INDIVIDUAL && request.risks().size() != 1) {
            throw new DomainRuleViolationException(
                    "INDIVIDUAL_REQUIRES_ONE_RISK",
                    "An individual policy must be created with exactly one risk"
            );
        }

        if (request.type() == PolicyType.INDIVIDUAL
                && !request.policyholderName().trim().equals(request.risks().getFirst().tenantName().trim())) {
            throw new DomainRuleViolationException("INDIVIDUAL_HOLDER_MUST_BE_TENANT",
                    "The individual policyholder and insured tenant must be the same person");
        }
        Policy policy = new Policy(
                request.type(),
                request.effectiveFrom(),
                request.durationMonths(),
                request.monthlyRent(),
                request.policyholderName(),
                request.beneficiaryName()
        );
        request.risks().stream()
                .map(this::toRisk)
                .forEach(policy::addRisk);

        Policy saved = policyRepository.saveAndFlush(policy);
        enqueueCoreUpdate(saved.getId());
        return PolicyResponse.from(saved);
    }

    @Transactional
    public PolicyResponse renew(long policyId, BigDecimal ipcPercentage) {
        Policy policy = requirePolicyForUpdate(policyId);
        policy.renew(ipcPercentage);
        enqueueCoreUpdate(policyId);
        policyRepository.flush();
        return PolicyResponse.from(policy);
    }

    @Transactional
    public PolicyResponse cancelPolicy(long policyId) {
        Policy policy = requirePolicyForUpdate(policyId);
        if (policy.getStatus() != PolicyStatus.CANCELADA) {
            policy.cancel();
            enqueueCoreUpdate(policyId);
            policyRepository.flush();
        }
        return PolicyResponse.from(policy);
    }

    @Transactional
    public RiskResponse addRisk(long policyId, RiskRequest request) {
        Policy policy = requirePolicyForUpdate(policyId);
        if (policy.getType() != PolicyType.COLECTIVA) {
            throw new DomainRuleViolationException(
                    "COLLECTIVE_POLICY_REQUIRED",
                    "Risks can be added only to a collective policy"
            );
        }
        Risk risk = toRisk(request);
        policy.addRisk(risk);
        policyRepository.flush();
        enqueueCoreUpdate(policyId);
        return RiskResponse.from(risk);
    }

    @Transactional
    public RiskResponse cancelRisk(long riskId) {
        long policyId = riskRepository.findPolicyIdByRiskId(riskId)
                .orElseThrow(() -> riskNotFound(riskId));
        requirePolicyForUpdate(policyId);
        Risk risk = requireRisk(riskId);
        if (risk.getStatus() != RiskStatus.CANCELADO) {
            risk.cancel();
            enqueueCoreUpdate(policyId);
            riskRepository.flush();
        }
        return RiskResponse.from(risk);
    }

    @Transactional(readOnly = true)
    public RiskResponse getRisk(long riskId) {
        return RiskResponse.from(requireRisk(riskId));
    }

    private Risk requireRisk(long riskId) {
        return riskRepository.findById(riskId).orElseThrow(() -> riskNotFound(riskId));
    }

    private ResourceNotFoundException riskNotFound(long riskId) {
        return new ResourceNotFoundException("RISK_NOT_FOUND", "Risk %d was not found".formatted(riskId));
    }

    private Policy requirePolicyForUpdate(long policyId) {
        return policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "POLICY_NOT_FOUND", "Policy %d was not found".formatted(policyId)));
    }

    private Policy requirePolicy(long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "POLICY_NOT_FOUND",
                        "Policy %d was not found".formatted(policyId)
                ));
    }

    private Risk toRisk(RiskRequest request) {
        return new Risk(request.propertyAddress(), request.tenantName());
    }

    private void enqueueCoreUpdate(Long policyId) {
        outboxRepository.save(new CoreOutboxEvent(policyId));
    }
}

