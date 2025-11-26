-- V1__init_schema.sql

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "citext";

-- ========================
-- ENUM TYPES
-- ========================

CREATE TYPE user_role AS ENUM ('ADMIN', 'TOURIST', 'GUIDE');

CREATE TYPE license_status AS ENUM ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED');

CREATE TYPE availability_status AS ENUM ('AVAILABLE', 'UNAVAILABLE', 'HOLD');

CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'EXPIRED');

CREATE TYPE review_moderation_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN');

CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'PUSH');

CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');

-- You can add more enums (e.g. hold_status, area_level, etc.) if you want strictness.

-- ========================
-- USERS
-- ========================

CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   TEXT   NOT NULL,
                       name            VARCHAR(200) NOT NULL,
                       phone           VARCHAR(30),
                       role            user_role NOT NULL DEFAULT 'TOURIST',
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                       last_login      TIMESTAMPTZ
);

-- ========================
-- GUIDES
-- ========================

CREATE TABLE guides (
                        id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        title                    VARCHAR(200),
                        bio                      TEXT,
                        languages                TEXT,              -- can store CSV or JSON string
                        per_day_rate             NUMERIC(10,2),
                        currency_code            VARCHAR(3),
                        profile_complete_score   INTEGER NOT NULL DEFAULT 0,
                        verified                 BOOLEAN NOT NULL DEFAULT FALSE,
                        photos                   TEXT,              -- JSON or CSV of URLs
                        created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
                        last_active_at           TIMESTAMPTZ
);

CREATE INDEX idx_guides_user_id ON guides(user_id);

-- ========================
-- AREAS
-- ========================

CREATE TABLE areas (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       country_code    VARCHAR(2) NOT NULL,
                       region_code     VARCHAR(50),
                       city            VARCHAR(200),
                       canonical_name  VARCHAR(255) NOT NULL,
                       level           VARCHAR(20) NOT NULL,       -- COUNTRY / REGION / CITY / SITE
                       geometry        TEXT
);

CREATE INDEX idx_areas_country_region_city
    ON areas(country_code, region_code, city);

-- ========================
-- LICENSES
-- ========================

CREATE TABLE licenses (
                          id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          guide_id            UUID NOT NULL REFERENCES guides(id) ON DELETE CASCADE,
                          license_type        VARCHAR(100) NOT NULL,
                          issuing_region_code VARCHAR(50),
                          license_number      VARCHAR(100),
                          issue_date          DATE,
                          expiry_date         DATE,
                          status              license_status NOT NULL DEFAULT 'PENDING',
                          admin_notes         TEXT,
                          doc_url             TEXT NOT NULL,
                          uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
                          verified_at         TIMESTAMPTZ
);

CREATE INDEX idx_licenses_guide_id ON licenses(guide_id);
CREATE INDEX idx_licenses_status ON licenses(status);
CREATE INDEX idx_licenses_expiry_date ON licenses(expiry_date);

-- ========================
-- GUIDE_AREAS
-- ========================

CREATE TABLE guide_areas (
                             id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             guide_id      UUID NOT NULL REFERENCES guides(id) ON DELETE CASCADE,
                             area_id       UUID NOT NULL REFERENCES areas(id) ON DELETE RESTRICT,
                             allowed_scope VARCHAR(50) NOT NULL,   -- e.g. CITY_ONLY / REGION_WIDE / NATIONAL
                             added_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                             CONSTRAINT uq_guide_area UNIQUE (guide_id, area_id)
);

CREATE INDEX idx_guide_areas_guide_id ON guide_areas(guide_id);
CREATE INDEX idx_guide_areas_area_id ON guide_areas(area_id);

-- ========================
-- AVAILABILITY_BLOCKS
-- ========================

CREATE TABLE availability_blocks (
                                     id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     guide_id    UUID NOT NULL REFERENCES guides(id) ON DELETE CASCADE,
                                     date        DATE NOT NULL,
                                     status      availability_status NOT NULL,
                                     reason      TEXT,
                                     created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     CONSTRAINT uq_availability_guide_date UNIQUE (guide_id, date)
);

CREATE INDEX idx_availability_guide_date
    ON availability_blocks(guide_id, date);

-- ========================
-- BOOKINGS
-- ========================

CREATE TABLE bookings (
                          id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          customer_id       UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                          guide_id          UUID NOT NULL REFERENCES guides(id) ON DELETE RESTRICT,
                          area_id           UUID NOT NULL REFERENCES areas(id) ON DELETE RESTRICT,
                          start_date        DATE NOT NULL,
                          end_date          DATE NOT NULL,
                          days_count        INTEGER NOT NULL CHECK (days_count > 0),
                          per_day_rate      NUMERIC(10,2) NOT NULL,
                          currency_code     VARCHAR(3) NOT NULL,
                          estimated_charge  NUMERIC(10,2) NOT NULL,
                          status            booking_status NOT NULL DEFAULT 'PENDING',
                          hold_token        UUID,
                          idempotency_token UUID,
                          meta              JSONB,
                          created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                          confirmed_at      TIMESTAMPTZ,
                          completed_at      TIMESTAMPTZ,
                          cancelled_at      TIMESTAMPTZ,
                          version           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_bookings_guide_dates
    ON bookings(guide_id, start_date, end_date);

CREATE INDEX idx_bookings_customer
    ON bookings(customer_id);

CREATE INDEX idx_bookings_status
    ON bookings(status);

-- ========================
-- HOLDS
-- ========================

-- Optional: a dedicated enum for hold status if you want strictness,
-- or reuse booking_status for simplicity.

CREATE TABLE holds (
                       id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       booking_id   UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                       guide_id     UUID NOT NULL REFERENCES guides(id) ON DELETE CASCADE,
                       start_date   DATE NOT NULL,
                       end_date     DATE NOT NULL,
                       hold_token   UUID NOT NULL,
                       expires_at   TIMESTAMPTZ NOT NULL,
                       status       booking_status NOT NULL DEFAULT 'PENDING',
                       created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_holds_guide_dates
    ON holds(guide_id, start_date, end_date);

-- ========================
-- REVIEWS
-- ========================

CREATE TABLE reviews (
                         id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         booking_id        UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                         guide_id          UUID NOT NULL REFERENCES guides(id) ON DELETE CASCADE,
                         customer_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         rating            SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         body              TEXT,
                         moderated_status  review_moderation_status NOT NULL DEFAULT 'PENDING',
                         created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                         CONSTRAINT uq_reviews_booking UNIQUE (booking_id)
);

CREATE INDEX idx_reviews_guide_id ON reviews(guide_id);
CREATE INDEX idx_reviews_customer_id ON reviews(customer_id);
CREATE INDEX idx_reviews_moderation ON reviews(moderated_status);

-- ========================
-- OUTBOX
-- ========================

CREATE TABLE outbox (
                        id             BIGSERIAL PRIMARY KEY,
                        aggregate_type VARCHAR(100) NOT NULL,
                        aggregate_id   UUID NOT NULL,
                        event_type     VARCHAR(100) NOT NULL,
                        payload        JSONB NOT NULL,
                        published      BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                        published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_published
    ON outbox(published, created_at);

-- ========================
-- NOTIFICATIONS
-- ========================

CREATE TABLE notifications (
                               id            BIGSERIAL PRIMARY KEY,
                               user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               channel       notification_channel NOT NULL,
                               template_key  VARCHAR(100) NOT NULL,
                               payload       JSONB NOT NULL,
                               status        notification_status NOT NULL DEFAULT 'PENDING',
                               sent_at       TIMESTAMPTZ,
                               created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_status
    ON notifications(user_id, status);

-- ========================
-- AUDIT_LOG
-- ========================

CREATE TABLE audit_log (
                           id             BIGSERIAL PRIMARY KEY,
                           actor_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
                           action         VARCHAR(100) NOT NULL,
                           before_state   JSONB,
                           after_state    JSONB,
                           resource_type  VARCHAR(100) NOT NULL,
                           resource_id    UUID NOT NULL,
                           created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_resource
    ON audit_log(resource_type, resource_id);
