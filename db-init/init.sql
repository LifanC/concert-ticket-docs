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
    ('USER_ITEM_IMPLEMENT', 'USER');

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
       );


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
                                                category IN ('音樂演唱會', '舞台劇', '展覽特展')
                                              ),
                                              CONSTRAINT activity_status_check CHECK (
                                                status IN ('即將開賣', '售票中', '已結束')
                                              )
);

CREATE TRIGGER trigger_activity_updated_date
BEFORE UPDATE
ON interviewworks_ticket.activity
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.session (
                                              id varchar NOT NULL,
                                              activity varchar NOT NULL,
                                              "date" varchar NOT NULL,
                                              "time" varchar NOT NULL,
                                              salesdate varchar NOT NULL,
                                              salestime varchar NOT NULL,
                                              capacity int8 NULL DEFAULT 0,
                                              sold int8 NULL DEFAULT 0,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT sessions_pk PRIMARY KEY (id)
);

CREATE TRIGGER trigger_session_updated_date
BEFORE UPDATE
ON interviewworks_ticket.session
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

CREATE TABLE IF NOT EXISTS interviewworks_ticket.ticket (
                                              orderno varchar NOT NULL,
                                              customer varchar NOT NULL,
                                              email varchar NOT NULL,
                                              "name" varchar NOT NULL,
                                              "date" varchar NOT NULL,
                                              "time" varchar NOT NULL,
                                              status varchar NOT NULL,
                                              price int8 NULL DEFAULT 0,
                                              payprice int8 NULL DEFAULT 0,
                                              created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT ticket_pk PRIMARY KEY (orderno),
                                              CONSTRAINT ticket_fk FOREIGN KEY (email) REFERENCES interviewworks_ticket.user_data(email)
);

CREATE TRIGGER trigger_ticket_updated_date
BEFORE UPDATE
ON interviewworks_ticket.ticket
FOR EACH ROW
EXECUTE FUNCTION interviewworks_ticket.update_updated_date();

-- 範例活動與場次資料（僅首次初始化資料庫時建立）
INSERT INTO interviewworks_ticket.activity
    (id, "name", category, "date", venue, status, price, description)
VALUES
    ('ACT-2026-001', '夏日星光音樂祭', '音樂演唱會', '2026-09-20', '台北流行音樂中心', '售票中', 1280, '戶外舞台演出，包含多組音樂人。'),
    ('ACT-2026-002', '經典舞台劇之夜', '舞台劇', '2026-10-05', '國家戲劇院', '即將開賣', 1680, '年度經典舞台劇特別場。')
ON CONFLICT (id) DO NOTHING;

INSERT INTO interviewworks_ticket.session
    (id, activity, "date", "time", salesdate, salestime, capacity, sold)
VALUES
    ('S-001', '夏日星光音樂祭', '2026-09-20', '19:30', '2026-08-15', '12:00', 180, 0),
    ('S-002', '夏日星光音樂祭', '2026-09-21', '19:30', '2026-08-15', '12:00', 118, 0),
    ('S-003', '經典舞台劇之夜', '2026-10-05', '14:30', '2026-09-01', '12:00', 80, 0)
ON CONFLICT (id) DO NOTHING;






















