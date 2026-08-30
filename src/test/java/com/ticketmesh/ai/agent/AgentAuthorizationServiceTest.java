package com.ticketmesh.ai.agent;

import com.ticketmesh.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentAuthorizationServiceTest {

    private AgentAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new AgentAuthorizationService(new AgentToolRegistry());
    }

    @Test
    void customerGetsReadToolsForOwnScopeOnly() {
        List<AgentTool> tools = service.toolsFor(User.Role.CUSTOMER);
        Set<String> names = toolNames(tools);

        assertTrue(names.contains("catalog.search"));
        assertTrue(names.contains("route.lookup"));
        assertTrue(names.contains("support.lookup"));
        assertTrue(names.contains("booking.status"));
        assertFalse(names.contains("pricing.check"));
        assertFalse(names.contains("admin.report"));
    }

    @Test
    void agentGainsPricingAccessButNotAdminReports() {
        List<AgentTool> tools = service.toolsFor(User.Role.AGENT);
        Set<String> names = toolNames(tools);

        assertTrue(names.contains("pricing.check"));
        assertFalse(names.contains("admin.report"));
        assertEquals(5, tools.size());
    }

    @Test
    void adminIsGrantedEveryRegisteredTool() {
        List<AgentTool> tools = service.toolsFor(User.Role.ADMIN);
        assertEquals(new AgentToolRegistry().allTools().size(), tools.size());
        assertTrue(toolNames(tools).contains("admin.report"));
    }

    @Test
    void isAllowed_gatesToolByRolePermission() {
        assertTrue(service.isAllowed(User.Role.CUSTOMER, "catalog.search"));
        assertTrue(service.isAllowed(User.Role.CUSTOMER, "booking.status"));
        assertFalse(service.isAllowed(User.Role.CUSTOMER, "pricing.check"));
        assertTrue(service.isAllowed(User.Role.AGENT, "pricing.check"));
        assertFalse(service.isAllowed(User.Role.AGENT, "admin.report"));
        assertTrue(service.isAllowed(User.Role.ADMIN, "admin.report"));
        assertTrue(service.isAllowed(User.Role.ADMIN, "catalog.search"));
    }

    @Test
    void isAllowed_deniesNullRoleUnknownToolAndNullTool() {
        assertFalse(service.isAllowed(null, "catalog.search"));
        assertFalse(service.isAllowed(User.Role.CUSTOMER, "not.a.tool"));
        assertFalse(service.isAllowed(User.Role.CUSTOMER, null));
    }

    @Test
    void toolsFor_nullRoleReturnsEmptyList() {
        assertEquals(List.of(), service.toolsFor(null));
    }

    private Set<String> toolNames(List<AgentTool> tools) {
        return tools.stream().map(AgentTool::name).collect(Collectors.toSet());
    }
}