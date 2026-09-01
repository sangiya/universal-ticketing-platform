package com.ticketmesh.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long bookingId;
    private double fare;
    private String qrData;

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private JsonNode performAndRead(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(builder).andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(1)
    void registerAndLogin() throws Exception {
        String resp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("username", "alice", "password", "strongpass123",
                                "fullName", "Alice Traveler", "email", "alice@example.com"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(resp);
        token = node.get("accessToken").asText();
        org.junit.jupiter.api.Assertions.assertTrue(token.length() > 20,
                "JWT token should be non-trivial");
    }

    @Test
    @Order(2)
    void searchTrainAndBook() throws Exception {
        String date = LocalDate.now().plusDays(1).toString();
        String searchResp = mockMvc.perform(get("/api/trains/search")
                        .param("date", date).param("origin", "Colombo").param("destination", "Kandy"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode schedules = objectMapper.readTree(searchResp);
        org.junit.jupiter.api.Assertions.assertTrue(schedules.isArray() && schedules.size() > 0);

        long scheduleId = schedules.get(0).get("id").asLong();
        fare = schedules.get(0).get("fare").asDouble();

        String bookResp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("scheduleId", scheduleId, "travelDate", date,
                                "passengerName", "Alice Traveler"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(bookResp);
        org.junit.jupiter.api.Assertions.assertEquals("RESERVED", booking.get("status").asText());
        org.junit.jupiter.api.Assertions.assertTrue(
                booking.get("bookingRef").asText().startsWith("ticketmesh-"));
        bookingId = booking.get("bookingId").asLong();
    }

    @Test
    @Order(3)
    void initiatePaymentIsPendingThenSettleAndGenerateQr() throws Exception {
        long paymentId = initiatePayment(bookingId, "4111111111111234", "1234");

        String statusResp = mockMvc.perform(get("/api/payments/booking/" + bookingId + "/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode status = objectMapper.readTree(statusResp);
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", status.get("paymentStatus").asText());
        org.junit.jupiter.api.Assertions.assertEquals("RESERVED", status.get("bookingStatus").asText());

        String settleResp = mockMvc.perform(post("/api/payments/" + paymentId + "/settle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("outcome", "SUCCESS", "reference", "GATEWAY-REF-1"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS",
                objectMapper.readTree(settleResp).get("status").asText());

        String ticketResp = mockMvc.perform(get("/api/tickets/booking/" + bookingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode ticket = objectMapper.readTree(ticketResp);
        org.junit.jupiter.api.Assertions.assertEquals("PAID", ticket.get("status").asText());
        qrData = ticket.get("qrData").asText();
        org.junit.jupiter.api.Assertions.assertTrue(qrData.contains("|"));
    }

    @Test
    @Order(4)
    void failedSettleKeepsBookingReservedAndAllowsRetry() throws Exception {
        long schedId = scheduleId();
        double bookFare = scheduleFare(schedId);

        String date = LocalDate.now().plusDays(1).toString();
        String bookResp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("scheduleId", schedId, "travelDate", date,
                                "passengerName", "Bob Retry"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bobBookingId = objectMapper.readTree(bookResp).get("bookingId").asLong();

        String paymentResp = mockMvc.perform(post("/api/payments/booking/" + bobBookingId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("amount", bookFare, "method", "CARD",
                                "cardNumber", "4111111111110000", "cardExpiry", "12/2",
                                "cardCvv", "123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long failedPaymentId = objectMapper.readTree(paymentResp).get("paymentId").asLong();

        String failResp = mockMvc.perform(post("/api/payments/" + failedPaymentId + "/settle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("outcome", "FAILED"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("FAILED",
                objectMapper.readTree(failResp).get("status").asText());

        long retryPaymentId = initiatePayment(bobBookingId, "4111111111110000", null);

        String settleResp = mockMvc.perform(post("/api/payments/" + retryPaymentId + "/settle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("outcome", "SUCCESS"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS",
                objectMapper.readTree(settleResp).get("status").asText());
    }

    @Test
    @Order(5)
    void verifyQrEndpoint() throws Exception {
        String verifyResp = mockMvc.perform(get("/api/tickets/verify").param("data", qrData))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode verification = objectMapper.readTree(verifyResp);
        org.junit.jupiter.api.Assertions.assertTrue(verification.get("valid").asBoolean());
        org.junit.jupiter.api.Assertions.assertEquals("Alice Traveler",
                verification.get("passengerName").asText());
    }

    @Test
    @Order(6)
    void cancelBookingRefundsAndFreesSeat() throws Exception {
        String cancelResp = mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED",
                objectMapper.readTree(cancelResp).get("status").asText());

        mockMvc.perform(post("/api/payments/booking/" + bookingId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("amount", fare, "method", "CARD"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    void unauthenticatedBookingIsRejected() throws Exception {
        String date = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("scheduleId", 1, "travelDate", date,
                                "passengerName", "No Auth"))))
                .andExpect(status().isUnauthorized());
    }

    private long initiatePayment(long bookingId, String cardNumber, String cardLast4) throws Exception {
        Map<String, Object> body = map("amount", fare, "method", "CARD",
                "cardNumber", cardNumber, "cardExpiry", "12/2", "cardCvv", "123");
        String initResp = mockMvc.perform(post("/api/payments/booking/" + bookingId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(initResp).get("paymentId").asLong();
    }

    private long scheduleId() throws Exception {
        String date = LocalDate.now().plusDays(1).toString();
        String resp = mockMvc.perform(get("/api/trains/search")
                        .param("date", date).param("origin", "Colombo").param("destination", "Kandy"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode schedules = objectMapper.readTree(resp);
        org.junit.jupiter.api.Assertions.assertTrue(schedules.size() > 0);
        return schedules.get(0).get("id").asLong();
    }

    private double scheduleFare(long scheduleId) throws Exception {
        String resp = mockMvc.perform(get("/api/trains/" + scheduleId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("fare").asDouble();
    }
}
