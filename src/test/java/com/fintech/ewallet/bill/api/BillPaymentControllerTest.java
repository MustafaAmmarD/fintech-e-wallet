package com.fintech.ewallet.bill.api;

import com.fintech.ewallet.bill.application.ExecuteBillPaymentUseCase;
import com.fintech.ewallet.bill.application.GetBillHistoryUseCase;
import com.fintech.ewallet.bill.application.GetBillersUseCase;
import com.fintech.ewallet.bill.application.PreviewBillPaymentUseCase;
import com.fintech.ewallet.bill.application.dto.BillExecuteResponse;
import com.fintech.ewallet.bill.application.dto.BillerResponse;
import com.fintech.ewallet.bill.domain.BillerCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for BillPaymentController using MockMvc.
 *
 * KEY CONCEPT: MockMvc simulates HTTP requests without a real web server.
 * It tests:
 * - HTTP status codes (200, 401, 403, etc.)
 * - JSON response structure
 * - Security annotations (@PreAuthorize)
 * - Required headers (Idempotency-Key)
 *
 * @SpringBootTest + @AutoConfigureMockMvc: Starts the full Spring context
 *                 but replaces Use Case beans with Mockito fakes
 *                 via @TestConfiguration.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BillPaymentControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public GetBillersUseCase getBillersUseCase() {
            return Mockito.mock(GetBillersUseCase.class);
        }

        @Bean
        @Primary
        public PreviewBillPaymentUseCase previewBillPaymentUseCase() {
            return Mockito.mock(PreviewBillPaymentUseCase.class);
        }

        @Bean
        @Primary
        public ExecuteBillPaymentUseCase executeBillPaymentUseCase() {
            return Mockito.mock(ExecuteBillPaymentUseCase.class);
        }

        @Bean
        @Primary
        public GetBillHistoryUseCase getBillHistoryUseCase() {
            return Mockito.mock(GetBillHistoryUseCase.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GetBillersUseCase getBillersUseCase;

    @Autowired
    private ExecuteBillPaymentUseCase executeBillPaymentUseCase;

    // ========================================================================
    // TEST 1: GET /billers should return 200 with a list of billers
    // ========================================================================
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /billers should return 200 with biller list")
    void getBillersShouldReturn200() throws Exception {
        // Arrange
        List<BillerResponse> billers = List.of(
                new BillerResponse(UUID.randomUUID(), "YEMEN_MOBILE", "Yemen Mobile", BillerCategory.TELECOM, "YER"),
                new BillerResponse(UUID.randomUUID(), "ADEN_NET", "Aden Net", BillerCategory.INTERNET, "YER"));
        when(getBillersUseCase.execute(any())).thenReturn(billers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bills/billers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("YEMEN_MOBILE"))
                .andExpect(jsonPath("$[0].name").value("Yemen Mobile"))
                .andExpect(jsonPath("$[1].code").value("ADEN_NET"));
    }

    // ========================================================================
    // TEST 2: POST /execute should return 200 with the payment result
    // ========================================================================
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /execute should return 200 with payment details")
    void executeBillPaymentShouldReturn200() throws Exception {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        BillExecuteResponse mockResponse = new BillExecuteResponse(
                paymentId, "BP-20260308-ABC123", "Yemen Mobile", BillerCategory.TELECOM,
                "770000000", new BigDecimal("1000"), new BigDecimal("50"),
                new BigDecimal("1050"), "YER", "COMPLETED", Instant.now());
        when(executeBillPaymentUseCase.execute(any(), any())).thenReturn(mockResponse);

        String requestBody = """
                {
                    "billerCode": "YEMEN_MOBILE",
                    "customerAccountNumber": "770000000",
                    "amount": 1000,
                    "currency": "YER"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/bills/execute")
                .with(csrf())
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.billerName").value("Yemen Mobile"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.feeAmount").value(50))
                .andExpect(jsonPath("$.totalDeducted").value(1050))
                .andExpect(jsonPath("$.referenceNo").value("BP-20260308-ABC123"));
    }

    // ========================================================================
    // TEST 3: POST /execute WITHOUT Idempotency-Key should return 400
    // ========================================================================
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /execute without Idempotency-Key should return 400")
    void executeBillPaymentWithoutIdempotencyKeyShouldFail() throws Exception {
        String requestBody = """
                {
                    "billerCode": "YEMEN_MOBILE",
                    "customerAccountNumber": "770000000",
                    "amount": 1000,
                    "currency": "YER"
                }
                """;

        mockMvc.perform(post("/api/v1/bills/execute")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // TEST 4: Unauthenticated user should get 403 on protected endpoints
    // ========================================================================
    @Test
    @DisplayName("POST /execute should return 403 for unauthenticated users")
    void shouldReturn403WhenNotAuthenticated() throws Exception {
        String requestBody = """
                {
                    "billerCode": "YEMEN_MOBILE",
                    "customerAccountNumber": "770000000",
                    "amount": 1000,
                    "currency": "YER"
                }
                """;

        mockMvc.perform(post("/api/v1/bills/execute")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }
}
