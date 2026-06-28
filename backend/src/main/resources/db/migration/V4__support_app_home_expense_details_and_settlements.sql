ALTER TABLE expense_groups
    ADD COLUMN icon VARCHAR(40),
    ADD COLUMN color VARCHAR(7),
    ADD CONSTRAINT ck_expense_groups_color_hex CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$');

ALTER TABLE expenses
    ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'general',
    ADD COLUMN note VARCHAR(500),
    ADD COLUMN occurred_on DATE NOT NULL DEFAULT CURRENT_DATE,
    ADD CONSTRAINT ck_expenses_category_not_blank CHECK (length(trim(category)) > 0);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES expense_groups(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL REFERENCES app_users(id),
    to_user_id UUID NOT NULL REFERENCES app_users(id),
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    note VARCHAR(500),
    settled_on DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_settlements_distinct_users CHECK (from_user_id <> to_user_id),
    CONSTRAINT ck_settlements_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_settlements_currency_uppercase CHECK (currency = upper(currency))
);

CREATE INDEX idx_settlements_group_settled_on ON settlements(group_id, settled_on DESC, created_at DESC);
CREATE INDEX idx_settlements_from_user_id ON settlements(from_user_id);
CREATE INDEX idx_settlements_to_user_id ON settlements(to_user_id);
