BEGIN;
SET LOCAL TIME ZONE 'Asia/Taipei';

CREATE SCHEMA IF NOT EXISTS interviewworks_ticket;

-- 建立 Trigger Function
CREATE OR REPLACE FUNCTION interviewworks_ticket.update_updated_date()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_date = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS interviewworks_ticket.permissions (
                                               code varchar(100) NULL,
                                               description varchar(100) NULL,
                                               CONSTRAINT permissions_pk PRIMARY KEY (code)
);
INSERT INTO interviewworks_ticket.permissions (code, description)
VALUES
    ('ADMIN_ITEM_IMPLEMENT', 'ADMIN'),
    ('USER_ITEM_IMPLEMENT', 'USER')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS interviewworks_ticket.user_data (
                                               account varchar PRIMARY KEY,
                                               "name" varchar NOT NULL,
                                               email varchar NOT NULL UNIQUE,
                                               phone varchar NULL,
                                               "password" varchar NOT NULL,
                                               created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               birthday varchar NULL,
                                               permissions varchar NULL
);
INSERT INTO interviewworks_ticket.user_data (account, name, email, phone, password, birthday, "permissions")
VALUES (
        'luke',
        'luke',
        'luke@admin.com',
        '0912345678',
        '$2a$10$rJR4wuJFsMtZjxUVgMR13.ET4tqMzWNJlbRgehR9CP./mVs1NIyF2',
        '',
        'ADMIN'
       ),
	   (
        'wangchen',
        '王曉明',
        'wang@user.com',
        '0912345678',
        '$2a$10$L5E9otCIMi3PY847s7K2LO4JO4clzb5kRn2adOZhtQxbQQEpfhWb6',
        '',
        'USER'
       ),
	   (
        'wangfan',
        '王曉明',
        'wang@test.com',
        '0912345678',
        '$2a$10$.ZaeO6pQB1oGsxHE7YUDrexVLp98CI0joV8klHJbGollTz7x9oLxG',
        '',
        'USER'
       ) ON CONFLICT DO NOTHING;


CREATE OR REPLACE TRIGGER trigger_user_data_updated_date
BEFORE UPDATE
ON interviewworks_ticket.user_data
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.secret (
                                              secret_number varchar(100) NOT NULL,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS interviewworks_ticket.activity (
                                              id varchar NOT NULL,
                                              "name" varchar NOT NULL,
											  category varchar NOT NULL,
                                              venue varchar NOT NULL,
                                              price numeric(12,2) NULL DEFAULT 0,
                                              description varchar NULL,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT activity_pk PRIMARY KEY (id),
                                              CONSTRAINT activity_category_check CHECK (
                                                category IN ('MUSIC_CONCERT', 'STAGE_PLAY', 'SPECIAL_EXHIBITION')
                                              )
);

CREATE OR REPLACE TRIGGER trigger_activity_updated_date
BEFORE UPDATE
ON interviewworks_ticket.activity
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.session (
    id varchar NOT NULL,
    activity_id varchar NOT NULL,
    "date" varchar NOT NULL,
    "time" varchar NOT NULL,
    salesdate varchar NOT NULL,
    salestime varchar NOT NULL,
    capacity int8 NOT NULL DEFAULT 0,
    reserved int8 NOT NULL DEFAULT 0,
    sold int8 NOT NULL DEFAULT 0,
    status varchar NOT NULL,
    created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT session_inventory_check
        CHECK (capacity >= 0 AND reserved >= 0 AND sold >= 0 AND reserved + sold <= capacity),
    CONSTRAINT sessions_pk
        PRIMARY KEY (id),
    CONSTRAINT sessions_activity_fk
        FOREIGN KEY (activity_id)
        REFERENCES interviewworks_ticket.activity(id),
    CONSTRAINT sessions_activity_id_id_uk
        UNIQUE (activity_id, id),
    CONSTRAINT session_status_check
        CHECK (
            status IN (
                'COMING_SOON',
                'TICKETS_ARE_ON_SALE',
                'SOLD_OUT',
                'ENDED'
            )
        )
);

CREATE OR REPLACE TRIGGER trigger_session_updated_date
BEFORE UPDATE
ON interviewworks_ticket.session
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.activity_favorite (
    user_email varchar NOT NULL,
    activity_id varchar NOT NULL,
    session_id varchar NOT NULL,
    created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT activity_favorite_pk
        PRIMARY KEY (user_email, activity_id, session_id),

    CONSTRAINT activity_favorite_user_fk
        FOREIGN KEY (user_email)
        REFERENCES interviewworks_ticket.user_data(email)
        ON DELETE CASCADE,

    CONSTRAINT activity_favorite_activity_fk
        FOREIGN KEY (activity_id)
        REFERENCES interviewworks_ticket.activity(id)
        ON DELETE CASCADE,

    CONSTRAINT activity_favorite_session_fk
        FOREIGN KEY (activity_id, session_id)
        REFERENCES interviewworks_ticket.session(activity_id, id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS interviewworks_ticket.ticket (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                              orderno varchar NOT NULL UNIQUE,
											  session_id varchar NOT NULL,
                                              email varchar NOT NULL,
                                              "name" varchar NOT NULL,
                                              "date" varchar NOT NULL,
                                              "time" varchar NOT NULL,
                                              status varchar NOT NULL,
											  seat varchar NOT NULL,
											  quantity int8 NULL DEFAULT 0,
                                              price numeric(12,2) NULL DEFAULT 0,
                                              payprice numeric(12,2) NULL DEFAULT 0,
											  expires_at timestamptz NULL,
											  paid_at timestamptz NULL,
											  cancelled_at timestamptz NULL,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
											  CONSTRAINT ticket_fk FOREIGN KEY (session_id) REFERENCES interviewworks_ticket.session(id),
                                              CONSTRAINT active_ticket_quantity_check CHECK (
                                                status NOT IN ('PENDING_PAYMENT', 'PAID') OR (quantity IS NOT NULL AND quantity = 1)
                                              ),
                                              CONSTRAINT pending_deadline_check CHECK (
                                                status <> 'PENDING_PAYMENT' OR expires_at IS NOT NULL
                                              ),
                                              CONSTRAINT ticket_status_check CHECK (
                                                status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED', 'REFUNDED')
                                              )
);

CREATE OR REPLACE TRIGGER trigger_ticket_updated_date
BEFORE UPDATE
ON interviewworks_ticket.ticket
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.activity_sequence
(
    activity_date DATE PRIMARY KEY,
    current_no INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS interviewworks_ticket.session_sequence
(
    session_date DATE PRIMARY KEY,
    current_no INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS interviewworks_ticket.ticket_sequence
(
    order_date DATE PRIMARY KEY,
    current_no INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS interviewworks_ticket.seat
(
    id varchar NOT NULL,
    activity_id varchar NOT NULL,
    seat_rows varchar NOT NULL,
    seats_per_row int8 DEFAULT 0,

    PRIMARY KEY (id, activity_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ticket_active_seat_uk ON interviewworks_ticket.ticket(session_id, seat)
    WHERE status IN ('PENDING_PAYMENT', 'PAID');
CREATE INDEX IF NOT EXISTS ticket_expiration_idx ON interviewworks_ticket.ticket(expires_at, orderno)
    WHERE status = 'PENDING_PAYMENT';

CREATE TABLE IF NOT EXISTS interviewworks_ticket.session_seat (
    session_id varchar NOT NULL REFERENCES interviewworks_ticket.session(id),
    seat_id varchar NOT NULL,
    status varchar NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'BLOCKED')),
    reserved_by_order varchar REFERENCES interviewworks_ticket.ticket(orderno),
    reserved_until timestamptz,
    version bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (session_id, seat_id),
    CHECK ((status = 'RESERVED' AND reserved_by_order IS NOT NULL AND reserved_until IS NOT NULL)
        OR (status = 'SOLD' AND reserved_by_order IS NOT NULL AND reserved_until IS NULL)
        OR (status IN ('AVAILABLE', 'BLOCKED') AND reserved_by_order IS NULL AND reserved_until IS NULL))
);
CREATE TABLE IF NOT EXISTS interviewworks_ticket.booking_idempotency (
    email varchar NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    request_hash varchar(64) NOT NULL,
    orderno varchar REFERENCES interviewworks_ticket.ticket(orderno),
    response_body text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (email, idempotency_key)
);

COMMIT;
