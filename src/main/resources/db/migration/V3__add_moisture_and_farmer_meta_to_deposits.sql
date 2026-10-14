-- V3__add_moisture_and_farmer_meta_to_deposits.sql
-- Adds moisture_percentage for spoilage risk tracking and farmer_name/farmer_phone
-- for recording walk-in farmer details when the farmer does not have a user account.

ALTER TABLE grain_deposits
    ADD COLUMN IF NOT EXISTS moisture_percentage NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS farmer_name        VARCHAR(150),
    ADD COLUMN IF NOT EXISTS farmer_phone       VARCHAR(30);
