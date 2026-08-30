CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE train_routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    origin VARCHAR(80) NOT NULL,
    destination VARCHAR(80) NOT NULL,
    base_fare DECIMAL(10, 2) NOT NULL,
    distance_km INT NOT NULL
);

CREATE TABLE train_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    train_code VARCHAR(20) NOT NULL,
    service_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    capacity INT NOT NULL,
    available_seats INT NOT NULL,
    fare DECIMAL(10, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_schedule_route FOREIGN KEY (route_id) REFERENCES train_routes (id)
);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_ref VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    travel_date DATE NOT NULL,
    passenger_name VARCHAR(120) NOT NULL,
    seat_number INT NOT NULL,
    fare DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP NULL,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_booking_schedule FOREIGN KEY (schedule_id) REFERENCES train_schedules (id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    method VARCHAR(20) NOT NULL,
    payment_ref VARCHAR(64) NOT NULL UNIQUE,
    provider_ref VARCHAR(40),
    card_last4 VARCHAR(16),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);
