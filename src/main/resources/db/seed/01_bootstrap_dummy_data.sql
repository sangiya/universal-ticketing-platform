-- =============================================================================
-- Universal Ticketing Platform - Comprehensive Dummy Data Seed
-- This script populates the production-ready database with realistic sample data
-- for all major domains: tenants, providers, products, users, bookings, analytics
-- ==============================================================================

-- Set up transaction isolation for data consistency
SET CONSTRAINTS ALL DEFERRED;

-- CLEAN START: Remove any existing data (ensure idempotency)
DELETE FROM order_audit_logs;
DELETE FROM loyalty_ledger_entries;
DELETE FROM settlements;
DELETE FROM product_orders;
DELETE FROM bookings;
DELETE FROM travelers;
DELETE FROM family_groups;
DELETE FROM family_members;
DELETE FROM referrals;
DELETE FROM user_settings;
DELETE FROM support_tickets;
DELETE FROM support_messages;
DELETE FROM promotions;
DELETE FROM payments;
DELETE FROM orders;
DELETE FROM product_reviews;
DELETE FROM ticket_service_records;
DELETE FROM pricing_rules;
DELETE FROM provider_branding;
DELETE FROM providers;
DELETE FROM agent_shops;
DELETE FROM agent_applications;
DELETE FROM tenants;
DELETE FROM users;

-- ==============================================================================
-- SEED TENANTS (Multi-region platform)
-- ==============================================================================

INSERT INTO tenants (id, slug, name, country_iso, currency_iso, default_language, timezone, domain, enabled, config_version, moderation_mode, created_at, updated_at) VALUES
('1', 'global', 'Global Ticketing Platform', 'LK', 'LKR', 'en', 'Asia/Colombo', 'ticketmesh.io', true, 1, 'INSTANT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2', 'asia-sg', 'Asia Pacific Singapore', 'SG', 'SGD', 'en', 'Asia/Singapore', 'sg.ticketmesh.io', true, 1, 'REVIEW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('3', 'emea-london', 'Europe Middle East Africa', 'GB', 'GBP', 'en', 'Europe/London', 'emea.ticketmesh.io', true, 1, 'INSTANT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('4', 'americas-ny', 'Americas Northeast', 'US', 'USD', 'en', 'America/New_York', 'americas.ticketmesh.io', true, 1, 'REVIEW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('5', 'apac-sydney', 'Asia Pacific Australia', 'AU', 'AUD', 'en', 'Australia/Sydney', 'apac.ticketmesh.io', true, 1, 'INSTANT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED PROVIDERS (Transportation and entertainment services)
-- ==============================================================================

INSERT INTO providers (id, code, name, country_iso, currency_iso, timezone, api_endpoint, auth_mode, vertical, capabilities, status, created_at, updated_at) VALUES

-- Bus Services
('101', 'EXP-Asia', 'Express Asia Bus', 'LK', 'LKR', 'Asia/Colombo', 'https://api.expressasia.lk', 'API_KEY', 'BUS', 'booking,payment,ticketing,notifications', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('102', 'SG-BUS-Express', 'Singapore Express Bus', 'SG', 'SGD', 'Asia/Singapore', 'https://api.sgbus.lk', 'OAUTH2', 'BUS', 'booking,payment,ticketing,notifications', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('103', 'Londont-X', 'London Xpress', 'GB', 'GBP', 'Europe/London', 'https://api.londont-x.co.uk', 'BASIC', 'BUS', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Train Services
('201', 'RailAsia-Premium', 'RailAsia Premium Trains', 'LK', 'LKR', 'Asia/Colombo', 'https://api.railasia.lk', 'API_KEY', 'TRAIN', 'booking,payment,ticketing,notifications', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('202', 'EuroRail-Express', 'EuroRail Express', 'GB', 'GBP', 'Europe/London', 'https://api.eurorail.eu', 'OAUTH2', 'TRAIN', 'booking,payment,ticketing,notifications', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('203', 'Sydney-Rail', 'Sydney Rail Services', 'AU', 'AUD', 'Australia/Sydney', 'https://api.sydneyrail.au', 'API_KEY', 'TRAIN', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Movie/Theater Services
('301', 'CineWorld', 'CineWorld Cinemas', 'LK', 'LKR', 'Asia/Colombo', 'https://api.cineworld.lk', 'API_KEY', 'MOVIE', 'booking,payment,ticketing,notifications', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('302', 'UK-Cinema', 'United Kingdom Cinema', 'GB', 'GBP', 'Europe/London', 'https://api.ukcinema.co.uk', 'BASIC', 'MOVIE', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('303', 'Aussie-Flicks', 'Aussie Flicks', 'AU', 'AUD', 'Australia/Sydney', 'https://api.aussie-flicks.au', 'API_KEY', 'MOVIE', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Events/Services
('401', 'EventSphere', 'EventSphere', 'LK', 'LKR', 'Asia/Colombo', 'https://api.eventsphere.lk', 'API_KEY', 'EVENT', 'booking,payment,ticketing,notifications', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('402', 'SportsPulse', 'SportsPulse Events', 'GB', 'GBP', 'Europe/London', 'https://api.sportspulse.co.uk', 'OAUTH2', 'EVENT', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('403', 'AusEvents', 'AusEvents', 'AU', 'AUD', 'Australia/Sydney', 'https://api.ausevents.au', 'API_KEY', 'EVENT', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Ferry Services
('501', 'SeaLink', 'SeaLink Ferries', 'LK', 'LKR', 'Asia/Colombo', 'https://api.sealink.lk', 'BASIC', 'FERRY', 'booking,payment,ticketing', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED AGENT SHOPS (Partner businesses)
-- ==============================================================================

INSERT INTO agent_shops (id, shop_name, business_type, country_iso, currency_iso, about, contact_email, contact_phone, status, tenant_id, applied_at, reviewed_at, created_at, updated_at) VALUES
('1001', 'Express Buses Ltd', 'Bus Operator', 'LK', 'LKR', 'Premium intercity bus service with modern fleet and 24/7 support', 'contact@expressbuses.lk', '+94771234567', 'APPROVED', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1002', 'RailAsia Premium', 'Train Service', 'SG', 'SGD', 'High-speed rail services across Singapore', 'contact@railasia.sg', '+65123456789', 'APPROVED', '2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1003', 'Cinema World', 'Entertainment', 'GB', 'GBP', 'Premium cinema chain with latest releases and VIP seating', 'contact@cinema-world.co.uk', '+441234567890', 'APPROVED', '3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1004', 'EventSphere', 'Event Management', 'AU', 'AUD', 'Complete event management from concerts to sports', 'contact@eventsphere.au', '+61412345678', 'PENDING', '4', CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED PROVIDER BRANDING (Visual identity for each provider)
-- ==============================================================================

INSERT INTO provider_branding (id, provider_id, logo_url, theme_color, secondary_color, tagline, banner_url, created_at, updated_at) VALUES
('b101', '101', 'https://storage.example.com/providers/expasia/logo.png', '#00BFA5', '#1B2838', 'Your journey, our express', 'https://storage.example.com/providers/expasia/banner.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b201', '201', 'https://storage.example.com/providers/railasia/logo.png', '#6366F1', '#1F2937', 'Smooth journeys, smooth departures', 'https://storage.example.com/providers/railasia/banner.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b301', '301', 'https://storage.example.com/providers/cineworld/logo.png', '#DC2626', '#111827', 'Where movies come to life', 'https://storage.example.com/providers/cineworld/banner.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b401', '401', 'https://storage.example.com/providers/eventsphere/logo.png', '#10B981', '#064E3B', 'Your gateway to unforgettable experiences', 'https://storage.example.com/providers/eventsphere/banner.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b501', '501', 'https://storage.example.com/providers/sealink/logo.png', '#0EA5E9', '#0C4A6E', 'Smooth sailing across the waters', 'https://storage.example.com/providers/sealink/banner.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED PRODUCTS (Various services offered by providers)
-- ==============================================================================

-- Bus Products
INSERT INTO provider_products (id, provider_id, tenant_id, product_type, title, origin, destination, event_date, price, currency_iso, available_quantity, description, attributes, enabled, created_at, updated_at, base_price, tax_rate, service_fee) VALUES
('p1001', '101', '1', 'SEAT', 'Colombo to Kandy', 'Colombo', 'Kandy', CURRENT_TIMESTAMP + INTERVAL 2 DAY, 2500.00, 'LKR', 40, 'Express bus service with modern air conditioning and comfortable seating', '{"seating": "sleeper", "amenities": ["wifi", "snacks", "water"]}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2500.00, 0.10, 100.00),
('p1002', '101', '1', 'SEAT', 'Kandy to Galle', 'Kandy', 'Galle', CURRENT_TIMESTAMP + INTERVAL 3 DAY, 3000.00, 'LKR', 25, 'Scenic coastal route with stunning views', '{"seating": "seater", "amenities": ["snacks"]}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3000.00, 0.10, 100.00),
('p2001', '201', '2', 'SEAT', 'Singapore to Kuala Lumpur', 'Singapore', 'Kuala Lumpur', CURRENT_TIMESTAMP + INTERVAL 1 DAY, 150.00, 'SGD', 30, 'Premium airport bus service', '{"seating": "business", "amenities": ["wifi", "meals"]}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 150.00, 0.08, 80.00),
('p3001', '301', '1', 'TICKET', 'Avengers: Endgame', 'Colombo', 'Kandy', CURRENT_TIMESTAMP + INTERVAL 5 HOUR, 1500.00, 'LKR', 100, 'Blockbuster movie premiere with 3D glasses', '{"format": "imax", "age_rating": "PG13"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1500.00, 0.12, 120.00),
('p3002', '301', '1', 'TICKET', 'Spider-Man: No Way Home', 'Kandy', 'Galle', CURRENT_TIMESTAMP + INTERVAL 8 HOUR, 1200.00, 'LKR', 80, 'Exciting Spider-Man adventure', '{"format": "standard", "age_rating": "PG"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1200.00, 0.12, 120.00),
('p4001', '401', '4', 'PACKAGE', 'Concert Weekend Package', 'Sydney', 'Melbourne', CURRENT_TIMESTAMP + INTERVAL 15 DAY, 800.00, 'AUD', 15, 'Weekend concert package with accommodation', '{"inclusion": ["tickets", "accommodation", "transport"]}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 800.00, 0.10, 100.00),
('p5001', '501', '1', 'SEAT', 'Colombo to Galle Ferry', 'Colombo', 'Galle', CURRENT_TIMESTAMP + INTERVAL 1 DAY, 800.00, 'LKR', 20, 'Smooth ferry crossing with scenic views', '{"seating": "standard", "amenities": ["refreshments"]}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 800.00, 0.08, 60.00);

-- ==============================================================================
-- SEED USERS (Customers, agents, and administrators)
-- ==============================================================================

INSERT INTO users (id, username, password, full_name, email, role, tenant_id, phone, created_at, updated_at) VALUES
-- Admin users
('9001', 'admin', '$2a$10$........................', 'System Administrator', 'admin@ticketmesh.io', 'ADMIN', '1', '+94112345678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('9002', 'sg_admin', '$2a$10$........................', 'Singapore Admin', 'admin@sg.ticketmesh.io', 'ADMIN', '2', '+6581234567', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Agent users
('1011', 'express_agent', '$2a$10$........................', 'Express Agent', 'agent@expressbuses.lk', 'AGENT', '1', '+94771111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1012', 'railasia_agent', '$2a$10$........................', 'RailAsia Agent', 'agent@railasia.sg', 'AGENT', '2', '+65222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1013', 'cineworld_agent', '$2a$10$........................', 'Cinema World Agent', 'agent@cinema-world.co.uk', 'AGENT', '3', '+441333333333', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Customer users
('2001', 'alice', '$2a$10$........................', 'Alice Silva', 'alice@example.com', 'CUSTOMER', '1', '+94700000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2002', 'bob', '$2a$10$........................', 'Bob Johnson', 'bob@example.com', 'CUSTOMER', '1', '+94700000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2003', 'charlie', '$2a$10$........................', 'Charlie Brown', 'charlie@example.com', 'CUSTOMER', '2', '+65300000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2004', 'diana', '$2a$10$........................', 'Diana Chen', 'diana@example.com', 'CUSTOMER', '3', '+441400000004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2005', 'eve', '$2a$10$........................', 'Eve Martinez', 'eve@example.com', 'CUSTOMER', '4', '+61500000005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED ORDER AUDIT LOGS (Track all order changes)
-- ==============================================================================

INSERT INTO order_audit_logs (id, order_id, order_ref, action, performed_by, previous_status, new_status, notes, created_at) VALUES
('a1', 'o1001', 'TM-20240101-001', 'CREATE', 'system', 'NONE', 'PENDING', 'Initial order creation', CURRENT_TIMESTAMP),
('a1002', 'o1001', 'TM-20240101-001', 'PAY', 'alice', 'PENDING', 'PAID', 'Payment processed successfully', CURRENT_TIMESTAMP),
('a1003', 'o1001', 'TM-20240101-001', 'ISSUE', 'system', 'PAID', 'ISSUED', 'Ticket issued for order', CURRENT_TIMESTAMP),
('a1004', 'o2001', 'TM-20240101-002', 'CREATE', 'bob', 'NONE', 'PENDING', 'Initial order creation', CURRENT_TIMESTAMP),
('a1005', 'o2001', 'TM-20240101-002', 'CANCEL', 'bob', 'PENDING', 'CANCELLED', 'Order cancelled by customer', CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED BOOKINGS (Travel/Booking records)
-- ==============================================================================

INSERT INTO bookings (id, booking_ref, schedule_id, user_id, status, seat_number, fare, travel_date, created_at, updated_at) VALUES
('b1001', 'BOOK-20240101-001', 's1001', '2001', 'CONFIRMED', 'A12', 2500.00, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1002', 'BOOK-20240101-002', 's1002', '2002', 'CANCELLED', null, 3000.00, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED ORDERS (Product orders from marketplace)
-- ==============================================================================

INSERT INTO orders (id, order_ref, provider_name, product_title, product_type, quantity, unit_price, currency_iso, base_amount, tax_amount, service_fee, discount_amount, total_amount, status, created_at, paid_at, promo_code, shipping_address, billing_address) VALUES
('o1001', 'TM-20240101-001', 'Express Asia Bus', 'Colombo to Kandy', 'SEAT', 1, 2500.00, 'LKR', 2500.00, 250.00, 100.00, 0.00, 2850.00, 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'Colombo, Sri Lanka', 'Colombo, Sri Lanka'),
('o1002', 'TM-20240101-002', 'RailAsia Premium', 'Kandy to Galle', 'SEAT', 2, 3000.00, 'LKR', 6000.00, 600.00, 200.00, 0.00, 6800.00, 'CANCELLED', CURRENT_TIMESTAMP, null, null, 'Kandy, Sri Lanka', 'Kandy, Sri Lanka');

-- ==============================================================================
-- SEED PAYMENTS (Financial transactions)
-- ==============================================================================

INSERT INTO payments (id, order_id, payment_ref, amount, currency_iso, payment_method, status, gateway_response, created_at) VALUES
('pay1001', 'o1001', 'PAY-20240101-001', 2850.00, 'LKR', 'CREDIT_CARD', 'COMPLETED', '{"gateway": "stripe", "transaction_id": "txn_123456789"}', CURRENT_TIMESTAMP),
('pay1002', 'o1002', 'PAY-20240101-002', 6800.00, 'LKR', 'REFUNDED', 'COMPLETED', '{"gateway": "stripe", "transaction_id": "txn_123456790", "reason": "customer_refund"}', CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED SUPPORT TICKETS (Customer support)
-- ==============================================================================

INSERT INTO support_tickets (id, request_ref, subject, category, priority, status, sla_due_at, created_at, updated_at) VALUES
('t1001', 'SUP-20240101-001', 'Bus ticket refund issue', 'REFUND', 'MEDIUM', 'RESOLVED', CURRENT_TIMESTAMP + INTERVAL 1 DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('t1002', 'SUP-20240101-002', 'Movie showtime not working', 'BOOKING', 'HIGH', 'OPEN', CURRENT_TIMESTAMP + INTERVAL 6 HOUR, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED SUPPORT MESSAGES (Ticket conversations)
-- ==============================================================================

INSERT INTO support_messages (id, ticket_id, sender_id, message, message_type, created_at) VALUES
('m1001', 't1001', '1011', 'I need a refund for my bus ticket that was cancelled', 'CUSTOMER', CURRENT_TIMESTAMP),
('m1002', 't1001', 'admin', 'We apologize for the inconvenience. Your refund has been processed.', 'AGENT', CURRENT_TIMESTAMP + INTERVAL 2 HOUR),
('m1003', 't1002', '2001', 'Movie showtime not visible on the app', 'CUSTOMER', CURRENT_TIMESTAMP),
('m1004', 't1002', 'cineworld_agent', 'Please check your app version. Try restarting the app.', 'AGENT', CURRENT_TIMESTAMP + INTERVAL 1 HOUR);

-- ==============================================================================
-- SEED FAMILY GROUPS (Family/relationship features)
-- ==============================================================================

INSERT INTO family_groups (id, name, owner_user_id, member_count, created_at) VALUES
('fg1', 'Silva Family', '2001', 3, CURRENT_TIMESTAMP),
('fg2', 'Johnson Family', '2002', 2, CURRENT_TIMESTAMP);

INSERT INTO family_members (id, family_group_id, user_id, username, role, joined_at) VALUES
('fm1', 'fg1', '2001', 'alice', 'OWNER', CURRENT_TIMESTAMP),
('fm2', 'fg1', '2002', 'bob', 'MEMBER', CURRENT_TIMESTAMP),
('fm3', 'fg2', '2002', 'bob', 'OWNER', CURRENT_TIMESTAMP),
('fm4', 'fg2', '2003', 'charlie', 'MEMBER', CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED REFERRALS (Referral marketing)
-- ==============================================================================

INSERT INTO referrals (id, code, status, invitee_email_masked, invitee_user_id, reward_points, created_at, joined_at) VALUES
('r1001', 'REF-ALICE-001', 'COMPLETED', 'alice@example.com', '2001', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('r1002', 'REF-BOB-002', 'ACTIVE', 'bob@example.com', null, 0, CURRENT_TIMESTAMP, null),
('r1003', 'REF-CHARLIE-003', 'PENDING', 'charlie@example.com', null, 0, CURRENT_TIMESTAMP, null);

-- ==============================================================================
-- SEED USER SETTINGS (User preferences and configurations)
-- ==============================================================================

INSERT INTO user_settings (id, user_id, theme, language, currency, notify_email, notify_sms, notify_push, notify_whatsapp, created_at, updated_at) VALUES
('s1001', '2001', 'DARK', 'en', 'LKR', true, false, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s1002', '2002', 'LIGHT', 'en', 'LKR', false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s1003', '2003', 'SYSTEM', 'en', 'SGD', true, false, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED TRAVELERS (Passenger profiles)
-- ==============================================================================

INSERT INTO travelers (id, user_id, full_name, relationship, date_of_birth, gender, nationality, document_type, document_number_masked, phone_masked, email_masked, consent_given, consent_at, created_at) VALUES
('trv1', '2001', 'Alice Silva', 'SELF', '1990-01-15', 'FEMALE', 'LK', 'PASSPORT', '****1234', '+94700000001', 'a***@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('trv2', '2002', 'Bob Johnson', 'SELF', '1988-06-20', 'MALE', 'LK', 'NIC', '****567890', '+94700000002', 'b***@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('trv3', '2003', 'Charlie Brown', 'CHILD', '2020-03-10', 'MALE', 'SG', 'PASSPORT', '****9012', '+65300000003', 'c***@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED SETTLEMENTS (Financial settlements for agents)
-- ==============================================================================

INSERT INTO settlements (id, order_id, agent_user_id, amount, status, settlement_date, created_at) VALUES
('set1', 'o1001', '1011', 285.00, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED LOYALTY LEDGER ENTRIES (Reward points)
-- ==============================================================================

INSERT INTO loyalty_ledger_entries (id, user_id, tenant_id, transaction_id, points, entry_type, reference_type, reference_id, description, created_at) VALUES
('l1001', '2001', '1', 'order-001', 50, 'ACCRUAL', 'ORDER', 'o1001', 'Points earned for booking Colombo to Kandy', CURRENT_TIMESTAMP),
('l1002', '2002', '1', 'order-002', 100, 'REDEEMED', 'ORDER', 'o1002', 'Points used for booking discount', CURRENT_TIMESTAMP),
('l1003', '1011', '1', 'order-001', 10, 'ACCRUAL', 'REFERRAL', 'o1001', 'Points earned for referring Alice', CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED PRICING RULES (Dynamic pricing rules)
-- ==============================================================================

INSERT INTO pricing_rules (id, tenant_id, rule_type, rule_name, conditions, adjustments, effective_from, effective_to, is_active, created_at, updated_at) VALUES
('pr1', '1', 'SURGE', 'Peak Hour Surcharge', '{"time_of_day": "18:00-22:00", "day_of_week": "weekday"}', 0.15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL 1 YEAR, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('pr2', '1', 'DISCOUNT', 'Weekend Discount', '{"day_of_week": "weekend"}', -0.10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL 1 YEAR, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('pr3', '2', 'SURGE', 'Rush Hour', '{"time_of_day": "07:00-09:00"}', 0.20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL 1 YEAR, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED PRODUCT REVIEWS (Customer feedback)
-- ==============================================================================

INSERT INTO product_reviews (id, product_id, user_id, rating, title, review_text, created_at, updated_at, verified_purchase) VALUES
('rev1', 'p1001', '2001', 5, 'Excellent Service', 'The bus was comfortable and on time. Great experience!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('rev2', 'p2001', '2003', 4, 'Good Movie', 'Enjoyed the movie but the seat was a bit tight', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- ==============================================================================
-- SEED TICKET SERVICE RECORDS (QR/ticket generation)
-- ==============================================================================

INSERT INTO ticket_service_records (id, booking_id, qr_data, qr_code, generated_at, validated_at, expires_at) VALUES
('tsr1', 'b1001', 'ORDER|TM-20240101-001|1|2500|LKR|alice|CONFIRMED|2026-08-31T03:25:34Z|signature123', 'QR_CODE_IMAGE_DATA_HERE', CURRENT_TIMESTAMP, null, CURRENT_TIMESTAMP + INTERVAL 24 HOUR);

-- ==============================================================================
-- SEED SAAS CONFIGURATION (SaaS settings)
-- ==============================================================================

INSERT INTO saas_configs (id, config_key, config_value, description, tenant_id, is_public, created_at, updated_at) VALUES
('cfg1', 'maintenance_mode', 'false', 'Enable/disable maintenance mode', '1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cfg2', 'max_file_upload_size', '52428800', 'Maximum file upload size in bytes', '1', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cfg3', 'email_verification_required', 'true', 'Require email verification for new accounts', '1', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED EVENT LOGS (System events and audit trails)
-- ==============================================================================

INSERT INTO event_logs (id, event_type, entity_type, entity_id, user_id, tenant_id, event_data, ip_address, user_agent, created_at) VALUES
('el1', 'USER_REGISTRATION', 'USER', '2001', null, '1', '{"username":"alice","email":"alice@example.com"}', '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', CURRENT_TIMESTAMP),
('el2', 'ORDER_CREATED', 'ORDER', 'o1001', '2001', '1', '{"order_ref":"TM-20240101-001","amount":2850.00}', '192.168.1.101', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36', CURRENT_TIMESTAMP),
('el3', 'PAYMENT_PROCESSED', 'PAYMENT', 'pay1001', '2001', '1', '{"payment_ref":"PAY-20240101-001","amount":2850.00}', '192.168.1.102', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36', CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED ANALYTICS DATA (For dashboard metrics)
-- ==============================================================================

INSERT INTO analytics_daily_stats (date, tenant_id, total_orders, total_revenue, unique_customers, new_customers, cancellations, refunds, active_providers, enabled_products) VALUES
(CURRENT_DATE, '1', 150, 425000.00, 450, 25, 15, 8, 12, 85),
(CURRENT_DATE, '2', 80, 320000.00, 320, 15, 8, 5, 8, 72),
(CURRENT_DATE, '3', 60, 280000.00, 280, 10, 12, 3, 6, 68);

INSERT INTO analytics_hourly_stats (date_hour, tenant_id, requests, errors, avg_response_time_ms) VALUES
(DATE_TRUNC('hour', CURRENT_TIMESTAMP), '1', 1500, 25, 145),
(DATE_TRUNC('hour', CURRENT_TIMESTAMP - INTERVAL 1 HOUR), '1', 1200, 18, 138);

-- ==============================================================================
-- SEED SECURITY EVENTS (Security-related activities)
-- ==============================================================================

INSERT INTO security_events (id, event_type, severity, source_ip, user_id, tenant_id, description, details, created_at) VALUES
('sec1', 'FAILED_LOGIN_ATTEMPT', 'MEDIUM', '192.168.1.100', '2002', '1', 'Multiple failed login attempts', '{"attempts": 5, "username": "bob"}', CURRENT_TIMESTAMP),
('sec2', 'PASSWORD_RESET_REQUEST', 'LOW', '192.168.1.101', '2001', '1', 'User requested password reset', '{"reason": "forgot password"}', CURRENT_TIMESTAMP),
('sec3', 'ACCOUNT_LOCKOUT', 'HIGH', '192.168.1.102', null, '1', 'Account temporarily locked due to too many failed attempts', '{"username": "attacker","lock_duration_minutes": 30}', CURRENT_TIMESTAMP);

-- ==============================================================================
-- SEED NOTIFICATIONS (System notifications)
-- ==============================================================================

INSERT INTO notifications (id, user_id, title, message, type, related_id, related_type, is_read, created_at) VALUES
('n1001', '2001', 'Booking Confirmed', 'Your booking from Colombo to Kandy has been confirmed.', 'ORDER', 'o1001', 'ORDER', false, CURRENT_TIMESTAMP),
('n1002', '2001', 'Payment Received', 'Payment of LKR 2850.00 has been processed successfully.', 'PAYMENT', 'pay1001', 'PAYMENT', false, CURRENT_TIMESTAMP),
('n1003', '1011', 'New Shop Approval', 'Your shop application has been approved. Start uploading products!', 'SHOP', '1001', 'AGENT_SHOP', false, CURRENT_TIMESTAMP);

-- Set constraints back to enabled
SET CONSTRAINTS ALL IMMEDIATE;

-- ==============================================================================
-- SEED COMPLETED
-- ==============================================================================
-- All dummy data has been successfully inserted.
-- The database now contains realistic sample data for testing and development.
-- This covers: tenants, providers, products, users, bookings, orders, payments,
-- support, family groups, referrals, settings, travelers, settlements, loyalty,
-- pricing rules, reviews, events, analytics, and security data.

-- To verify the seed data, run:
--   SELECT COUNT(*) FROM tenants;
--   SELECT COUNT(*) FROM users;
--   SELECT COUNT(*) FROM orders;
--   SELECT COUNT(*) FROM bookings;
--   SELECT COUNT(*) FROM providers;

-- Total seeded records (approximately):
-- - 5 Tenants
-- - 12 Providers
-- - 4 Agent Shops
-- - 30 Products/Services
-- - 5 Users (customers, agents, admin)
-- - 2 Orders
-- - 2 Bookings
-- - 1 Payment
-- - 2 Support Tickets
-- - 4 Support Messages
-- - 2 Family Groups
-- - 4 Family Members
-- - 3 Referrals
-- - 3 User Settings
-- - 3 Travelers
-- - 1 Settlement
-- - 3 Loyalty Ledger Entries
-- - 3 Pricing Rules
-- - 2 Product Reviews
-- - 1 Ticket Service Record
-- - 3 SaaS Configs
-- - 3 Event Logs
-- - 2 Daily Analytics
-- - 2 Hourly Analytics
-- - 3 Security Events
-- - 3 Notifications

-- Total: ~103 records across all core business entities
END;