package com.ticketmesh.ai.dto;

import com.ticketmesh.ai.agent.AgentTool;

import java.util.List;

public record AgentToolsResponse(String role, List<AgentTool> tools) {
}