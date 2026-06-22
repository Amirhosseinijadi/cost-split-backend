CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_display_name_not_blank CHECK (length(trim(display_name)) > 0),
    CONSTRAINT ck_app_users_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE expense_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_expense_groups_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE TABLE group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES expense_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_group_members_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES expense_groups(id) ON DELETE CASCADE,
    description VARCHAR(200) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    paid_by_user_id UUID NOT NULL REFERENCES app_users(id),
    split_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_expenses_description_not_blank CHECK (length(trim(description)) > 0),
    CONSTRAINT ck_expenses_total_positive CHECK (total_amount > 0),
    CONSTRAINT ck_expenses_currency_uppercase CHECK (currency = upper(currency)),
    CONSTRAINT ck_expenses_split_type CHECK (split_type IN ('EQUAL'))
);

CREATE TABLE expense_shares (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id),
    amount_owed NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uq_expense_shares_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT ck_expense_shares_amount_non_negative CHECK (amount_owed >= 0)
);

CREATE INDEX idx_group_members_user_id ON group_members(user_id);
CREATE INDEX idx_expenses_group_created_at ON expenses(group_id, created_at DESC);
CREATE INDEX idx_expenses_paid_by_user_id ON expenses(paid_by_user_id);
CREATE INDEX idx_expense_shares_user_id ON expense_shares(user_id);

