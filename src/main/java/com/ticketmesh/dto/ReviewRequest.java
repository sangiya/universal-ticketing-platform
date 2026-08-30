package com.ticketmesh.dto;

public record ReviewRequest(Long productId, int rating, String title, String comment) {
}