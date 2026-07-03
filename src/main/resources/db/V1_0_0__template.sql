DROP TABLE IF EXISTS orders CASCADE;

CREATE TABLE orders (
    id uuid NOT NULL,
    created_at timestamptz NOT NULL,
    modified_at timestamptz NOT NULL,
    user_ref varchar(128) NOT NULL,
    ref varchar(64),
    created_on timestamptz,
    due_on timestamptz,
    msisdn varchar(16) NOT NULL,
    state_id integer NOT NULL,
    stage_ref varchar(64) NOT NULL,
    cancelable boolean NOT NULL,
    updatable boolean NOT NULL,
    error_code integer,
    error_ref varchar(64),
    error_info varchar(512),
    tariff_ref varchar(64),
    tariff_name varchar(512)
);

ALTER TABLE orders
    ADD CONSTRAINT pk_orders
        PRIMARY KEY (id);

CREATE INDEX ix_orders_user_ref
    ON orders (user_ref);

CREATE UNIQUE INDEX ix_orders_ref_not_null
    ON orders (ref)
    WHERE ref IS NOT NULL;

CREATE INDEX ix_orders_state_id
    ON orders (state_id);

DROP TABLE IF EXISTS orders_history CASCADE;

CREATE TABLE orders_history (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    modified_at timestamptz NOT NULL,
    user_ref varchar(128) NOT NULL,
    ref varchar(64),
    created_on timestamptz,
    due_on timestamptz,
    msisdn varchar(16) NOT NULL,
    state_id integer NOT NULL,
    stage_ref varchar(64) NOT NULL,
    cancelable boolean NOT NULL,
    updatable boolean NOT NULL,
    error_code integer,
    error_ref varchar(64),
    error_info varchar(512),
    tariff_ref varchar(64),
    tariff_name varchar(512)
);

ALTER TABLE orders_history
    ADD CONSTRAINT pk_orders_history
        PRIMARY KEY (id);

CREATE INDEX ix_orders_history_order_id
    ON orders_history (order_id);

ALTER TABLE orders_history
    ADD CONSTRAINT fk_orders_history_order_id
        FOREIGN KEY (order_id)
            REFERENCES orders (id);

DROP TABLE IF EXISTS documents CASCADE;

CREATE TABLE documents (
    id uuid,
    created_at timestamptz NOT NULL,
    modified_at timestamptz NOT NULL,
    user_ref varchar(128) NOT NULL,
    ref varchar(64),
    state_id integer NOT NULL,
    error_code integer,
    error_ref varchar(64),
    error_info varchar(512)
);

ALTER TABLE documents
    ADD CONSTRAINT pk_documents
        PRIMARY KEY (id);

CREATE INDEX ix_documents_ref
    ON documents (ref);

DROP TABLE IF EXISTS flows CASCADE;

CREATE TABLE flows (
    id uuid NOT NULL,
    created_at timestamptz NOT NULL,
    modified_at timestamptz NOT NULL,
    user_ref varchar(128) NOT NULL,
    state_id integer NOT NULL,
    stage_ref varchar(64) NOT NULL,
    error_code integer,
    error_ref varchar(64),
    tariff_ref varchar(32),
    tariff_name varchar(512),
    msisdn varchar(16),
    token varchar(32),
    iccid varchar(32),
    document_id uuid,
    order_id uuid
);

ALTER TABLE flows
    ADD CONSTRAINT pk_flows
        PRIMARY KEY (id);

CREATE INDEX ix_flows_user_ref
    ON flows (user_ref);

CREATE INDEX ix_flows_order_id
    ON flows (order_id);

ALTER TABLE flows
    ADD CONSTRAINT fk_flows_order_id
        FOREIGN KEY (order_id)
            REFERENCES orders (id);

CREATE INDEX ix_flows_document_id
    ON flows (document_id);

ALTER TABLE flows
    ADD CONSTRAINT fk_flows_document_id
        FOREIGN KEY (document_id)
            REFERENCES documents (id);

DROP TABLE IF EXISTS flows_history CASCADE;

CREATE TABLE flows_history (
    id uuid NOT NULL,
    flow_id uuid NOT NULL,
    modified_at timestamptz NOT NULL,
    user_ref varchar(128) NOT NULL,
    state_id integer NOT NULL,
    stage_ref varchar(64) NOT NULL,
    error_code integer,
    error_ref varchar(64),
    tariff_ref varchar(32),
    tariff_name varchar(512),
    msisdn varchar(16),
    token varchar(32),
    iccid varchar(32),
    document_id uuid,
    order_id uuid
);

ALTER TABLE flows_history
    ADD CONSTRAINT pk_flows_history
        PRIMARY KEY (id);

CREATE INDEX ix_flows_history_flow_id
    ON flows_history (flow_id);

ALTER TABLE flows_history
    ADD CONSTRAINT fk_flows_history_flow_id
        FOREIGN KEY (flow_id)
            REFERENCES flows (id);

CREATE INDEX ix_flows_history_order_id
    ON flows_history (order_id);

ALTER TABLE flows_history
    ADD CONSTRAINT fk_flows_history_order_id
        FOREIGN KEY (order_id)
            REFERENCES orders (id);

CREATE INDEX ix_flows_history_document_id
    ON flows_history (document_id);

ALTER TABLE flows_history
    ADD CONSTRAINT fk_flows_history_document_id
        FOREIGN KEY (document_id)
            REFERENCES documents (id);

DROP TABLE IF EXISTS shedlock CASCADE;

CREATE TABLE shedlock (
    name varchar(64) NOT NULL,
    lock_until timestamptz NOT NULL,
    locked_at timestamptz NOT NULL,
    locked_by varchar(255) NOT NULL
);

ALTER TABLE shedlock
    ADD CONSTRAINT pk_shedlock
        PRIMARY KEY (name);
