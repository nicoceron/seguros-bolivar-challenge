package com.segurosbolivar.policy.repository;

import com.segurosbolivar.policy.domain.Policy;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PolicyRepository
        extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select p from Policy p where p.id = :id")
    Optional<Policy> findByIdForUpdate(@Param("id") long id);
}
