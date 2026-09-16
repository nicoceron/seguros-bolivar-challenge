package com.segurosbolivar.policy.repository;

import com.segurosbolivar.policy.domain.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    List<Risk> findByPolicyIdOrderByIdAsc(Long policyId);

    // Scalar lookup avoids loading stale risk state before taking the aggregate lock.
    @Query("select r.policy.id from Risk r where r.id = :id")
    Optional<Long> findPolicyIdByRiskId(@Param("id") long id);
}

