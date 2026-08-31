package com.ticketmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shop_moderation_audit")
public class ShopModerationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "from_status", nullable = false, length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(length = 255)
    private String reason;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ShopModerationAudit() {}

    public ShopModerationAudit(Long shopId, String fromStatus, String toStatus, String reason, Long actorUserId) {
        this.shopId = shopId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.actorUserId = actorUserId;
        this.createdAt = Instant.now();
    }
}
