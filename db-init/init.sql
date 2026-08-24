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
                                               "name" varchar NOT NULL,
                                               email varchar NOT NULL,
                                               phone varchar NULL,
                                               "password" varchar NOT NULL,
                                               created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               birthday varchar NULL,
                                               permissions varchar NULL,
                                               CONSTRAINT user_data_pk PRIMARY KEY (email)
);
INSERT INTO interviewworks_ticket.user_data (name, email, phone, password, birthday, "permissions")
VALUES (
        'luke',
        'luke@admin.com',
        '0912345678',
        '$2a$10$rJR4wuJFsMtZjxUVgMR13.ET4tqMzWNJlbRgehR9CP./mVs1NIyF2',
        '',
        'ADMIN'
       ) ON CONFLICT (email) DO NOTHING;


CREATE TRIGGER trigger_user_data_updated_date
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
                                              "date" varchar NOT NULL,
                                              venue varchar NOT NULL,
                                              status varchar NOT NULL,
                                              price int8 NULL DEFAULT 0,
                                              description varchar NULL,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT activity_pk PRIMARY KEY (id),
                                              CONSTRAINT activity_category_check CHECK (
                                                category IN ('MUSIC_CONCERT', 'STAGE_PLAY', 'SPECIAL_EXHIBITION')
                                              ),
                                              CONSTRAINT activity_status_check CHECK (
                                                status IN ('COMING_SOON', 'TICKETS_ARE_ON_SALE', 'ENDED')
                                              )
);

CREATE TRIGGER trigger_activity_updated_date
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
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT sessions_pk PRIMARY KEY (id),
											  CONSTRAINT sessions_fk FOREIGN KEY (activity_id) REFERENCES interviewworks_ticket.activity(id)
);

CREATE TRIGGER trigger_session_updated_date
BEFORE UPDATE
ON interviewworks_ticket.session
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.ticket (
                                              orderno varchar NOT NULL,
											  session_id varchar NOT NULL,
                                              customer varchar NOT NULL,
                                              email varchar NOT NULL,
                                              "name" varchar NOT NULL,
                                              "date" varchar NOT NULL,
                                              "time" varchar NOT NULL,
                                              status varchar NOT NULL,
											  quantity int8 NULL DEFAULT 0,
                                              price int8 NULL DEFAULT 0,
                                              payprice int8 NULL DEFAULT 0,
											  expires_at timestamp NULL,
											  paid_at timestamp NULL,
											  cancelled_at timestamp NULL,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT ticket_pk PRIMARY KEY (orderno),
											  CONSTRAINT ticket_fk FOREIGN KEY (session_id) REFERENCES interviewworks_ticket.session(id),
                                              CONSTRAINT ticket_status_check CHECK (
                                                status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED', 'REFUNDED')
                                              )
);

CREATE TRIGGER trigger_ticket_updated_date
BEFORE UPDATE
ON interviewworks_ticket.ticket
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();
























