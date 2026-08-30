package com.ticketmesh.ai.agent;

import com.ticketmesh.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Maps a caller role to the set of AI tool permissions it carries and answers
 * whether a given tool may be invoked for that role. The permission matrix is
 * an explicit allow-list: unlisted permissions are denied by default.
 */
@Service
public class AgentAuthorizationService {

    private final AgentToolRegistry registry;

    public AgentAuthorizationService(AgentToolRegistry registry) {
        this.registry = registry;
    }

    public boolean isAllowed(User.Role role, String toolName) {
        if (role == null || toolName == null) {
            return false;
        }
        AgentTool tool = findTool(toolName);
        if (tool == null) {
            return false;
        }
        return permissionsFor(role).contains(tool.requiresPermission());
    }

    public List<AgentTool> toolsFor(User.Role role) {
        if (role == null) {
            return List.of();
        }
        Set<String> permissions = permissionsFor(role);
        return registry.allTools().stream()
                .filter(tool -> permissions.contains(tool.requiresPermission()))
                .toList();
    }

    private AgentTool findTool(String toolName) {
        for (AgentTool tool : registry.allTools()) {
            if (tool.name().equals(toolName)) {
                return tool;
            }
        }
        return null;
    }

    private Set<String> permissionsFor(User.Role role) {
        return switch (role) {
            case CUSTOMER -> Set.of("catalog:read", "support:read", "booking:read");
            case AGENT -> Set.of("catalog:read", "pricing:read", "support:read", "booking:read");
            case ADMIN -> Set.of("catalog:read", "pricing:read", "support:read",
                    "booking:read", "admin:read");
        };
    }
}