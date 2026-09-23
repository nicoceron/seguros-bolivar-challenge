// Purpose of this file: Reads risks and finds their parent policy before a change.
package com.segurosbolivar.policy.repository;

import com.segurosbolivar.policy.domain.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    /** Lists a policy’s risks in ID order. */
    List<Risk> findByPolicyIdOrderByIdAsc(Long policyId);

    // Scalar lookup avoids loading stale risk state before taking the aggregate lock.
    @Query("select r.policy.id from Risk r where r.id = :id")
    /** Gets only the parent ID so the policy can be locked before changing a risk. */
    Optional<Long> findPolicyIdByRiskId(@Param("id") long id);
}

