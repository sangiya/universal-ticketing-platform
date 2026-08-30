package com.ticketmesh.ai.controller;

import com.ticketmesh.ai.agent.AgentAuthorizationService;
import com.ticketmesh.ai.agent.AgentTool;
import com.ticketmesh.ai.dto.AgentToolsResponse;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AgentToolsController {

    private static final Logger log = LoggerFactory.getLogger(AgentToolsController.class);

    private final AgentAuthorizationService authorizationService;
    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    public AgentToolsController(AgentAuthorizationService authorizationService,
                                CurrentUser currentUser,
                                UserRepository userRepository) {
        this.authorizationService = authorizationService;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }

    @GetMapping("/tools")
    public ResponseEntity<AgentToolsResponse> tools() {
        User user = resolveUser();
        User.Role role = user == null ? null : user.getRole();
        List<AgentTool> authorized = role == null
                ? List.of()
                : authorizationService.toolsFor(role);
        return ResponseEntity.ok(
                new AgentToolsResponse(role == null ? null : role.name(), authorized));
    }

    private User resolveUser() {
        try {
            return userRepository.findByUsername(currentUser.username()).orElse(null);
        } catch (RuntimeException ex) {
            log.debug("No authenticated user for agent tools request");
            return null;
        }
    }
}