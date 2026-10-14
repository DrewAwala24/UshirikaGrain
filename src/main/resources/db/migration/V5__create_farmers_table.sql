-- V5__create_farmers_table.sql
-- Create dedicated farmers entity table and integrate with deposits

CREATE TABLE IF NOT EXISTS farmers (
    id BIGSERIAL PRIMARY KEY,
    farmer_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    national_id VARCHAR(50) UNIQUE,
    phone_number VARCHAR(50) NOT NULL,
    email VARCHAR(150),
    county VARCHAR(100),
    sub_county VARCHAR(100),
    ward VARCHAR(100),
    location VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    registered_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_farmers_status ON farmers(status);
CREATE INDEX IF NOT EXISTS idx_farmers_farmer_code ON farmers(farmer_code);
CREATE INDEX IF NOT EXISTS idx_farmers_phone_number ON farmers(phone_number);
CREATE INDEX IF NOT EXISTS idx_farmers_national_id ON farmers(national_id);

-- Link grain_deposits to farmers table
ALTER TABLE grain_deposits ADD COLUMN IF NOT EXISTS farmer_record_id BIGINT REFERENCES farmers(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_grain_deposits_farmer_record_id ON grain_deposits(farmer_record_id);

-- Seed initial farmers
INSERT INTO farmers (id, farmer_code, full_name, national_id, phone_number, email, county, sub_county, ward, location, status, user_id, registered_by_user_id)
VALUES
(1, 'FRM-2026-0001', 'John Kamau', '12345678', '+254711000003', 'john.kamau@farms.com', 'Trans Nzoia', 'Kiminini', 'Sikhendu', 'Plot 44, Sikhendu Scheme', 'ACTIVE', 3, 1),
(2, 'FRM-2026-0002', 'Grace Wanjiku', '23456789', '0722112233', 'grace.wanjiku@farms.co.ke', 'Trans Nzoia', 'Kitale Central', 'Hospital', 'Bidii Farm, Kitale', 'ACTIVE', NULL, 1),
(3, 'FRM-2026-0003', 'Kiprono Cheruiyot', '34567890', '0733445566', 'kiprono.c@farms.co.ke', 'Trans Nzoia', 'Endebess', 'Matumbei', 'Chepchoina Block 2', 'ACTIVE', NULL, 1)
ON CONFLICT (farmer_code) DO NOTHING;

-- Advance sequence
SELECT setval('farmers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM farmers));

-- Backfill any existing deposits matching farmer phone or name
UPDATE grain_deposits gd
SET farmer_record_id = f.id
FROM farmers f
WHERE gd.farmer_record_id IS NULL AND (gd.farmer_phone = f.phone_number OR gd.farmer_id = f.user_id);
