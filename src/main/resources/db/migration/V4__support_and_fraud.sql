CREATE TABLE support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_ref VARCHAR(30) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    assignee_id BIGINT,
    subject VARCHAR(200) NOT NULL,
    category VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(2000),
    sla_due_at TIMESTAMP NOT NULL,
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_support_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_support_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_support_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
);

CREATE TABLE support_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_support_msg_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id),
    CONSTRAINT fk_support_msg_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE fraud_signals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    subject_ref VARCHAR(60) NOT NULL,
    actor_username VARCHAR(50),
    score INT NOT NULL,
    risk VARCHAR(12) NOT NULL,
    flags VARCHAR(500),
    details LONGTEXT,
    created_at TIMESTAMP NOT NULL
);
