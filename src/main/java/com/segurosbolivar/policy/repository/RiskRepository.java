package com.segurosbolivar.policy.repository;

import com.segurosbolivar.policy.domain.Risk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    List<Risk> findByPolicyIdOrderByIdAsc(Long policyId);
}

