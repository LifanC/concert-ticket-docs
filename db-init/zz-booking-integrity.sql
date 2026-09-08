BEGIN;
SET LOCAL TIME ZONE 'Asia/Taipei';

ALTER TABLE interviewworks_ticket.session ADD CONSTRAINT session_inventory_check
    CHECK (capacity >= 0 AND reserved >= 0 AND sold >= 0 AND reserved + sold <= capacity);
ALTER TABLE interviewworks_ticket.ticket ADD CONSTRAINT active_ticket_quantity_check
    CHECK (status NOT IN ('PENDING_PAYMENT', 'PAID') OR (quantity IS NOT NULL AND quantity = 1));
ALTER TABLE interviewworks_ticket.ticket ADD CONSTRAINT pending_deadline_check
    CHECK (status <> 'PENDING_PAYMENT' OR expires_at IS NOT NULL);
ALTER TABLE interviewworks_ticket.activity ALTER COLUMN price TYPE numeric(12,2);
ALTER TABLE interviewworks_ticket.ticket ALTER COLUMN price TYPE numeric(12,2);
ALTER TABLE interviewworks_ticket.ticket ALTER COLUMN payprice TYPE numeric(12,2);
ALTER TABLE interviewworks_ticket.ticket ALTER COLUMN expires_at TYPE timestamptz USING expires_at AT TIME ZONE 'Asia/Taipei';
ALTER TABLE interviewworks_ticket.ticket ALTER COLUMN paid_at TYPE timestamptz USING paid_at AT TIME ZONE 'Asia/Taipei';
ALTER TABLE interviewworks_ticket.ticket ALTER COLUMN cancelled_at TYPE timestamptz USING cancelled_at AT TIME ZONE 'Asia/Taipei';

-- Fail instead of silently discarding duplicate historical active reservations.
CREATE UNIQUE INDEX ticket_active_seat_uk ON interviewworks_ticket.ticket(session_id, seat)
    WHERE status IN ('PENDING_PAYMENT', 'PAID');
CREATE INDEX ticket_expiration_idx ON interviewworks_ticket.ticket(expires_at, orderno)
    WHERE status = 'PENDING_PAYMENT';

CREATE TABLE interviewworks_ticket.session_seat (
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
INSERT INTO interviewworks_ticket.session_seat(session_id, seat_id)
SELECT DISTINCT s.id, trim(r.label) || '-' || lpad(n.num::text, 2, '0')
FROM interviewworks_ticket.session s
JOIN interviewworks_ticket.seat layout ON layout.activity_id = s.activity_id
CROSS JOIN LATERAL unnest(string_to_array(layout.seat_rows, ',')) r(label)
CROSS JOIN LATERAL generate_series(1, least(layout.seats_per_row, 99)::int) n(num)
ON CONFLICT DO NOTHING;
INSERT INTO interviewworks_ticket.session_seat(session_id, seat_id)
SELECT DISTINCT session_id, seat FROM interviewworks_ticket.ticket
ON CONFLICT DO NOTHING;
UPDATE interviewworks_ticket.session_seat ss
SET status = CASE WHEN t.status = 'PAID' THEN 'SOLD' ELSE 'RESERVED' END,
    reserved_by_order = t.orderno,
    reserved_until = CASE WHEN t.status = 'PENDING_PAYMENT' THEN t.expires_at END
FROM interviewworks_ticket.ticket t
WHERE t.session_id = ss.session_id AND t.seat = ss.seat_id AND t.status IN ('PENDING_PAYMENT', 'PAID');

CREATE TABLE interviewworks_ticket.booking_idempotency (
    email varchar NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    request_hash varchar(64) NOT NULL,
    orderno varchar REFERENCES interviewworks_ticket.ticket(orderno),
    response_body text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (email, idempotency_key)
);

COMMIT;
