// Purpose of this file: Checks invalid input, security, missing IDs, and repeated cancellations over HTTP.
package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.domain.Policy;
import com.segurosbolivar.policy.domain.PolicyType;
import com.segurosbolivar.policy.domain.Risk;
import com.segurosbolivar.policy.repository.CoreOutboxEventRepository;
import com.segurosbolivar.policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(properties = {"app.demo-data.enabled=false", "app.core.dispatch-enabled=false"})
class PolicyApiEdgeCasesTest {
    @Autowired MockMvc mvc;
    @Autowired PolicyRepository policies;
    @Autowired CoreOutboxEventRepository outbox;
    private long policyId;
    private long riskId;
    private static final String RISK = "{\"propertyAddress\":\"Calle 1\",\"tenantName\":\"Ana\"}";

    @BeforeEach
    /** Prepares test data or the local test server before each case. */
    void setUp() {
        Policy p = new Policy(PolicyType.COLECTIVA, LocalDate.of(2026, 1, 1),
                12, new BigDecimal("100.01"), "Inmobiliaria", "Propietario");
        p.addRisk(new Risk("Calle 1", "Ana"));
        policies.saveAndFlush(p);
        policyId = p.getId();
        riskId = p.getRisks().getFirst().getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/polizas/1/renovar", "/polizas/1/cancelar", "/polizas/1/riesgos",
            "/riesgos/1/cancelar", "/core-mock/evento", "/polizas"})
    /** Checks every data-changing route requires the key. */
    void everyMutationRejectsMissingKey(String path) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertThat(outbox.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "wrong", "1234567"})
    /** Checks incorrect keys return 401. */
    void incorrectKeysAreRejected(String key) throws Exception {
        mvc.perform(get("/polizas").header("x-api-key", key)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"size,0", "size,101", "page,-1", "tipo,UNKNOWN", "estado,UNKNOWN"})
    /** Checks invalid filters and page settings return 400. */
    void invalidFiltersAndPaginationReturn400(String name, String value) throws Exception {
        mvc.perform(get("/polizas").header("x-api-key", "123456").param(name, value))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"ipcPercentage\":-1}", "{\"ipcPercentage\":101}", "{broken"})
    /** Checks invalid renewal does not queue a CORE event. */
    void invalidRenewalDoesNotWriteAnEvent(String body) throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", policyId).header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(outbox.count()).isZero();
    }

    @Test
    /** Checks missing IDs return 404. */
    void unknownResourcesReturn404() throws Exception {
        mvc.perform(get("/polizas/999999").header("x-api-key", "123456")).andExpect(status().isNotFound());
        mvc.perform(get("/polizas/999999/riesgos").header("x-api-key", "123456")).andExpect(status().isNotFound());
        mvc.perform(post("/riesgos/999999/cancelar").header("x-api-key", "123456")).andExpect(status().isNotFound());
        mvc.perform(post("/polizas/999999/renovar").header("x-api-key", "123456")
                .contentType(MediaType.APPLICATION_JSON).content("{\"ipcPercentage\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    /** Checks repeated policy cancellation does not duplicate events. */
    void repeatedPolicyCancellationDoesNotDuplicateEvents() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/polizas/{id}/cancelar", policyId).header("x-api-key", "123456"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
        }
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    /** Checks repeated risk cancellation does not duplicate events. */
    void repeatedRiskCancellationDoesNotDuplicateEvents() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/riesgos/{id}/cancelar", riskId).header("x-api-key", "123456"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        }
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    /** Checks a cancelled collective policy rejects a new risk. */
    void cannotAddARiskToACancelledCollective() throws Exception {
        mvc.perform(post("/polizas/{id}/cancelar", policyId).header("x-api-key", "123456"));
        mvc.perform(post("/polizas/{id}/riesgos", policyId).header("x-api-key", "123456")
                .contentType(MediaType.APPLICATION_JSON).content(RISK))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POLICY_CANCELLED"));
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    /** Checks a created risk has an ID and a readable URL. */
    void createdRiskHasAnIdAndRetrievableLocation() throws Exception {
        String location = mvc.perform(post("/polizas/{id}/riesgos", policyId).header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON).content(RISK))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(location).header("x-api-key", "123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tenantName").value("Ana"));
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    /** Checks both individual-policy creation rules. */
    void individualCreationEnforcesExactlyOneRiskAndTenantAsHolder() throws Exception {
        mvc.perform(post("/polizas").header("x-api-key", "123456").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("INDIVIDUAL", "Ana", "[" + RISK + "]")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.riskCount").value(1));
        mvc.perform(post("/polizas").header("x-api-key", "123456").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("INDIVIDUAL", "Ana", "[" + RISK + "," + RISK + "]")))
                .andExpect(status().isConflict());
        mvc.perform(post("/polizas").header("x-api-key", "123456").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("INDIVIDUAL", "Other person", "[" + RISK + "]")))
                .andExpect(status().isConflict());
        assertThat(outbox.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]"})
    /** Checks missing risks cause 400 rather than an internal 500. */
    void emptyOrNullRisksAreRejectedWithout500(String risks) throws Exception {
        mvc.perform(post("/polizas").header("x-api-key", "123456").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("COLECTIVA", "Agency", risks)))
                .andExpect(status().isBadRequest());
        assertThat(outbox.count()).isZero();
    }

    @Test
    /** Checks a safe tracking ID is returned to the client. */
    void correlationIdIsReturnedAndInvalidValuesAreReplaced() throws Exception {
        mvc.perform(get("/polizas").header("x-api-key", "123456").header("X-Correlation-ID", "test-123"))
                .andExpect(header().string("X-Correlation-ID", "test-123"));
        String actual = mvc.perform(get("/polizas").header("x-api-key", "123456")
                        .header("X-Correlation-ID", "invalid trace id"))
                .andReturn().getResponse().getHeader("X-Correlation-ID");
        assertThat(actual).matches("[0-9a-f-]{36}");
    }

    /** Builds the policy creation JSON shared by several tests. */
    private String createBody(String type, String holder, String risks) {
        return """
                {"type":"%s","effectiveFrom":"2026-01-01","durationMonths":12,
                 "monthlyRent":100.01,"policyholderName":"%s","beneficiaryName":"Owner","risks":%s}
                """.formatted(type, holder, risks);
    }
}
