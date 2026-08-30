package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * A single membership of a user in a family group. The owning user holds the
 * OWNER role and can administer the group.
 */
@Entity
@Table(name = "family_members")
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public FamilyMember() {
    }

    public FamilyMember(Long familyId, Long userId, Role role) {
        this.familyId = familyId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public enum Role {
        OWNER,
        MEMBER
    }
}
