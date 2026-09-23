// Purpose of this file: Creates two sample policies when the database is empty so the demo has data.
package com.segurosbolivar.policy.config;

import com.segurosbolivar.policy.domain.Policy;
import com.segurosbolivar.policy.domain.PolicyType;
import com.segurosbolivar.policy.domain.Risk;
import com.segurosbolivar.policy.repository.PolicyRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "app.demo-data.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DemoDataInitializer implements ApplicationRunner {

    private final PolicyRepository policyRepository;

    /** Receives database access so sample policies can be inserted. */
    public DemoDataInitializer(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional
    /** At startup, inserts one individual and one collective policy only if none exist. */
    public void run(ApplicationArguments args) {
        if (policyRepository.count() > 0) {
            return;
        }

        Policy individual = new Policy(
                PolicyType.INDIVIDUAL,
                LocalDate.of(2026, 1, 1),
                12,
                new BigDecimal("1800000.00"),
                "Ana Torres",
                "Carlos Ruiz"
        );
        individual.addRisk(new Risk("Calle 72 # 10-20, Bogota", "Ana Torres"));

        Policy collective = new Policy(
                PolicyType.COLECTIVA,
                LocalDate.of(2026, 2, 1),
                12,
                new BigDecimal("2500000.00"),
                "Inmobiliaria Horizonte SAS",
                "Maria Gomez"
        );
        collective.addRisk(new Risk("Carrera 15 # 93-45, Bogota", "Luis Diaz"));
        collective.addRisk(new Risk("Calle 100 # 19-61, Bogota", "Sofia Rojas"));

        policyRepository.saveAll(List.of(individual, collective));
    }
}

