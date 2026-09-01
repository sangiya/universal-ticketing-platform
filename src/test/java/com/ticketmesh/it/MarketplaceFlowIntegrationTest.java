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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end marketplace flow: platform admin opens a tenant, an agent applies
 * for a shop (Uber/PickMe model), the admin approves, the agent connects a
 * provider and uploads ticket inventory, and a customer searches the public
 * catalog.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MarketplaceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String agentToken;
    private Long shopId;
    private Long tenantId;

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode read(MockHttpServletRequestBuilder builder, int expectedStatus) throws Exception {
        String body = mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    @Order(1)
    void adminLoginAndCreateTenant() throws Exception {
        JsonNode resp = read(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("username", "admin", "password", "ChangeMe123!"))), 200);
        adminToken = resp.get("accessToken").asText();
        assertTrue(adminToken.length() > 20);

        JsonNode tenant = read(post("/api/admin/tenants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("slug", "demo-sg", "name", "Demo Singapore",
                        "countryIso", "SG", "currencyIso", "SGD",
                        "defaultLanguage", "en", "timezone", "Asia/Singapore"))), 201);

        tenantId = tenant.get("id").asLong();
        assertEquals("demo-sg", tenant.get("slug").asText());
        assertEquals("INSTANT", tenant.get("moderationMode").asText());

        JsonNode moderated = read(put("/api/admin/tenants/demo-sg/moderation?mode=REVIEW")
                .header("Authorization", "Bearer " + adminToken), 200);
        assertEquals("REVIEW", moderated.get("moderationMode").asText());
    }

    @Test
    @Order(2)
    void applyWhiteLabelBranding() throws Exception {
        JsonNode branding = read(put("/api/admin/tenants/demo-sg/branding")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("brandName", "Travigo", "primaryColor", "#00BFA5",
                        "secondaryColor", "#1B2838", "logoUrl",
                        "https://cdn.example.com/travigo.svg",
                        "borderRadius", 12, "darkMode", true))), 200);

        assertEquals("Travigo", branding.get("brandName").asText());
        assertEquals("#00BFA5", branding.get("primaryColor").asText());
        assertTrue(branding.get("darkMode").asBoolean());

        JsonNode pub = read(get("/api/tenant/demo-sg/branding"), 200);
        assertEquals("Travigo", pub.get("brandName").asText());
    }

    @Test
    @Order(3)
    void agentRegistersAppliesShopAndAdminApproves() throws Exception {
        JsonNode agentReg = read(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("username", "shopOwner", "password", "strongpass123",
                        "fullName", "Shop Owner", "email", "owner@example.com",
                        "role", "AGENT", "tenantSlug", "demo-sg"))), 201);
        agentToken = agentReg.get("accessToken").asText();
        assertEquals("AGENT", agentReg.get("role").asText());

        JsonNode shop = read(post("/api/agent/shops?tenant=demo-sg")
                .header("Authorization", "Bearer " + agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("shopName", "Express Buses", "businessType", "Bus Operator",
                        "countryIso", "SG", "currencyIso", "SGD",
                        "about", "Premium intercity bus service"))), 201);
        shopId = shop.get("id").asLong();
        assertEquals("PENDING", shop.get("status").asText());

        JsonNode approved = read(put("/api/admin/shops/" + shopId + "?action=APPROVED")
                .header("Authorization", "Bearer " + adminToken), 200);
        assertEquals("APPROVED", approved.get("status").asText());
    }

    @Test
    @Order(4)
    void agentConnectsProviderAndUploadsInventory() throws Exception {
        JsonNode provider = read(post("/api/agent/providers")
                .header("Authorization", "Bearer " + agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("code", "EXP-AGENT", "name", "Express Buses",
                        "apiEndpoint", "https://express.example.com/api",
                        "authMode", "API_KEY", "vertical", "BUS",
                        "capabilities", "SEARCH,BOOKING,TICKETS"))), 201);
        assertEquals("BUS", provider.get("vertical").asText());
        assertEquals("API_KEY", provider.get("authMode").asText());

        JsonNode product = read(post("/api/agent/providers/EXP-AGENT/products")
                .header("Authorization", "Bearer " + agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(map("productType", "SEAT", "title", "City A to City B",
                        "origin", "City A", "destination", "City B",
                        "price", 25.50, "currencyIso", "SGD",
                        "availableQuantity", 40, "description", "Comfort coach seat"))), 201);
        assertEquals(40, product.get("availableQuantity").asInt());
        assertEquals("Express Buses", product.get("providerName").asText());
        assertEquals("SEAT", product.get("productType").asText());
    }

    @Test
    @Order(5)
    void customerSearchesPublicCatalog() throws Exception {
        JsonNode catalog = read(get("/api/catalog?tenantId=" + tenantId + "&type=SEAT"), 200);
        assertTrue(catalog.isArray());
        assertTrue(catalog.size() >= 1);
        assertEquals("City A to City B", catalog.get(0).get("title").asText());
        assertEquals("Express Buses", catalog.get(0).get("providerName").asText());
    }

    @Test
    @Order(6)
    void adminDashboardReflectsActivity() throws Exception {
        JsonNode stats = read(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken), 200);
        assertTrue(stats.get("agents").asLong() >= 1);
        assertTrue(stats.get("approvedShops").asLong() >= 1);
        assertTrue(stats.get("activeTenants").asLong() >= 1);
        assertTrue(stats.get("totalProducts").asLong() >= 1);
    }

    @Test
    @Order(7)
    void catalogAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/catalog?tenantId=" + tenantId))
                .andExpect(status().isOk());
    }
}
