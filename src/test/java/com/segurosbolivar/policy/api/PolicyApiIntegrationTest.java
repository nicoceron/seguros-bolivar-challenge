// Purpose of this file: Checks the main HTTP requirements: listing, renewal, cancellation, risks, and CORE mock.
package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.domain.Policy;
import com.segurosbolivar.policy.domain.PolicyType;
import com.segurosbolivar.policy.domain.Risk;
import com.segurosbolivar.policy.repository.CoreOutboxEventRepository;
import com.segurosbolivar.policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.demo-data.enabled=false",
        "app.core.dispatch-enabled=false"
})
class PolicyApiIntegrationTest {

    private static final String API_KEY = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private CoreOutboxEventRepository outboxRepository;

    private Long individualId;
    private Long collectiveId;
    private Long collectiveRiskId;

    @BeforeEach
    /** Prepares test data or the local test server before each case. */
    void setUp() {
        Policy individual = policy(PolicyType.INDIVIDUAL, "1000000.00");
        individual.addRisk(new Risk("Individual address", "Individual tenant"));

        Policy collective = policy(PolicyType.COLECTIVA, "2000000.00");
        collective.addRisk(new Risk("Collective address", "Collective tenant"));

        policyRepository.saveAndFlush(individual);
        policyRepository.saveAndFlush(collective);
        individualId = individual.getId();
        collectiveId = collective.getId();
        collectiveRiskId = collective.getRisks().getFirst().getId();
    }

    @Test
    /** Checks the API rejects a request without the required key. */
    void requiresApiKey() throws Exception {
        mockMvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
    }

    @Test
    /** Checks type/status filters and paginated results. */
    void listsPoliciesUsingSpanishFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/polizas")
                        .header("x-api-key", API_KEY)
                        .queryParam("tipo", "INDIVIDUAL")
                        .queryParam("estado", "ACTIVA"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVA"));
    }

    @Test
    /** Checks new rent, premium, status, and pending CORE event after renewal. */
    void renewsPolicyUsingIpcAndEnqueuesCoreUpdate() throws Exception {
        mockMvc.perform(post("/polizas/{id}/renovar", individualId)
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ipcPercentage": 5.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RENOVADA"))
                .andExpect(jsonPath("$.monthlyRent").value(1050000.00))
                .andExpect(jsonPath("$.premium").value(12600000.00));

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    /** Checks renewing a cancelled policy returns a conflict. */
    void cannotRenewACancelledPolicy() throws Exception {
        mockMvc.perform(post("/polizas/{id}/cancelar", individualId)
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk());

        mockMvc.perform(post("/polizas/{id}/renovar", individualId)
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ipcPercentage": 5.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_CANCELLED"));
    }

    @Test
    /** Checks cancelling a policy also cancels every risk. */
    void cancellingPolicyCancelsAllRisks() throws Exception {
        mockMvc.perform(post("/polizas/{id}/cancelar", collectiveId)
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        mockMvc.perform(get("/polizas/{id}/riesgos", collectiveId)
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CANCELADO"));
    }

    @Test
    /** Checks collective policies accept new risks and individual ones reject them. */
    void addsRiskOnlyToCollectivePolicy() throws Exception {
        String risk = """
                {
                  "propertyAddress": "New address",
                  "tenantName": "New tenant"
                }
                """;

        mockMvc.perform(post("/polizas/{id}/riesgos", collectiveId)
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(risk))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVO"));

        mockMvc.perform(post("/polizas/{id}/riesgos", individualId)
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(risk))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COLLECTIVE_POLICY_REQUIRED"));
    }

    @Test
    /** Checks cancelling one risk does not cancel the whole policy. */
    void cancelsOneRisk() throws Exception {
        mockMvc.perform(post("/riesgos/{id}/cancelar", collectiveRiskId)
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    /** Checks the mock accepts the required JSON and responds 202. */
    void coreMockAcceptsTheRequiredPayload() throws Exception {
        mockMvc.perform(post("/core-mock/evento")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "evento": "ACTUALIZACION",
                                  "polizaId": 555
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    /** Builds a small policy so each test can run independently. */
    private Policy policy(PolicyType type, String monthlyRent) {
        return new Policy(
                type,
                LocalDate.of(2026, 1, 1),
                12,
                new BigDecimal(monthlyRent),
                "Policyholder",
                "Beneficiary"
        );
    }
}

