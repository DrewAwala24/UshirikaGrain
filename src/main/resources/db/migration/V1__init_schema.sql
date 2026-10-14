-- V1__init_schema.sql
-- Initial database schema for ZuriAgro grain deposit and inventory platform

-- 1. Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. User Roles Join Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Storage Depots Table
CREATE TABLE IF NOT EXISTS storage_depots (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    capacity_metric_tonnes NUMERIC(15, 2) NOT NULL CHECK (capacity_metric_tonnes > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Grain Types Table
CREATE TABLE IF NOT EXISTS grain_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Grain Deposits Table
CREATE TABLE IF NOT EXISTS grain_deposits (
    id BIGSERIAL PRIMARY KEY,
    deposit_reference VARCHAR(64) NOT NULL UNIQUE,
    farmer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    depot_id BIGINT NOT NULL REFERENCES storage_depots(id) ON DELETE RESTRICT,
    grain_type_id BIGINT NOT NULL REFERENCES grain_types(id) ON DELETE RESTRICT,
    quantity NUMERIC(15, 2) NOT NULL CHECK (quantity > 0),
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    rejection_reason VARCHAR(500),
    notes VARCHAR(1000),
    created_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reviewed_by_user_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    deposited_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Inventory Balances Table (Current on-hand and reserved quantity)
CREATE TABLE IF NOT EXISTS inventory_balances (
    id BIGSERIAL PRIMARY KEY,
    depot_id BIGINT NOT NULL REFERENCES storage_depots(id) ON DELETE RESTRICT,
    grain_type_id BIGINT NOT NULL REFERENCES grain_types(id) ON DELETE RESTRICT,
    quantity NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (quantity >= 0),
    reserved_quantity NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (reserved_quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_depot_grain UNIQUE (depot_id, grain_type_id)
);

-- 8. Inventory Movements Table (Immutable audit ledger of all stock transactions)
CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    movement_reference VARCHAR(64) NOT NULL UNIQUE,
    depot_id BIGINT NOT NULL REFERENCES storage_depots(id) ON DELETE RESTRICT,
    grain_type_id BIGINT NOT NULL REFERENCES grain_types(id) ON DELETE RESTRICT,
    movement_type VARCHAR(50) NOT NULL CHECK (movement_type IN ('DEPOSIT_ACCEPTANCE', 'WITHDRAWAL', 'ADJUSTMENT_ADD', 'ADJUSTMENT_SUBTRACT')),
    quantity NUMERIC(15, 2) NOT NULL CHECK (quantity > 0),
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    previous_balance NUMERIC(15, 2) NOT NULL CHECK (previous_balance >= 0),
    new_balance NUMERIC(15, 2) NOT NULL CHECK (new_balance >= 0),
    source_deposit_id BIGINT REFERENCES grain_deposits(id) ON DELETE RESTRICT,
    reference_number VARCHAR(100),
    reason VARCHAR(500) NOT NULL,
    authorized_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(50) NOT NULL,
    performed_by_username VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance and query optimization
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_deposits_farmer ON grain_deposits(farmer_id);
CREATE INDEX IF NOT EXISTS idx_deposits_depot ON grain_deposits(depot_id);
CREATE INDEX IF NOT EXISTS idx_deposits_status ON grain_deposits(status);
CREATE INDEX IF NOT EXISTS idx_deposits_created_at ON grain_deposits(created_at);
CREATE INDEX IF NOT EXISTS idx_inventory_depot_grain ON inventory_balances(depot_id, grain_type_id);
CREATE INDEX IF NOT EXISTS idx_movements_depot_grain ON inventory_movements(depot_id, grain_type_id);
CREATE INDEX IF NOT EXISTS idx_movements_created_at ON inventory_movements(created_at);
CREATE INDEX IF NOT EXISTS idx_movements_source_deposit ON inventory_movements(source_deposit_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_name, entity_id);
