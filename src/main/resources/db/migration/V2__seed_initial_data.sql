-- V2__seed_initial_data.sql
-- Seed baseline roles, grain types, reference depots, and default system users
-- Passwords below are hashed for 'Admin@12345' using BCrypt ($2a$10$...)

-- 1. Insert Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'System administrator with full operational and user management permissions'),
(2, 'ROLE_DEPOT_MANAGER', 'Depot manager with deposit processing and inventory management permissions'),
(3, 'ROLE_FARMER', 'Registered agricultural depositor with deposit submission and tracking permissions')
ON CONFLICT (name) DO NOTHING;

-- 2. Insert Grain Types
INSERT INTO grain_types (id, code, name, description, unit_of_measure, active) VALUES
(1, 'MAIZE', 'Grade 1 White Maize', 'Dry white maize grain suitable for commercial milling', 'KG', TRUE),
(2, 'WHEAT', 'Hard Red Winter Wheat', 'High-protein bread wheat grain', 'KG', TRUE),
(3, 'SOYBEAN', 'Yellow Soybean', 'Oilseed soybean grain for processing and feed', 'KG', TRUE),
(4, 'SORGHUM', 'White Sorghum', 'Drought-tolerant white sorghum grain', 'KG', TRUE),
(5, 'RICE', 'Long Grain Milled Rice', 'Grade 1 aromatic long grain rice', 'KG', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 3. Insert Storage Depots
INSERT INTO storage_depots (id, code, name, location, capacity_metric_tonnes, active) VALUES
(1, 'ELD-DEP-01', 'Eldoret North Grain Terminal', 'Eldoret Industrial Area, Uasin Gishu County', 75000.00, TRUE),
(2, 'NAK-DEP-01', 'Nakuru Rift Valley Silo', 'Nakuru West Logistics Park, Nakuru County', 45000.00, TRUE),
(3, 'NAI-DEP-01', 'Nairobi Central Strategic Grain Depot', 'Nairobi Commercial Zone, Nairobi County', 60000.00, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 4. Insert Initial Users
-- Password for all seed users is 'Admin@12345'
INSERT INTO users (id, username, email, password_hash, full_name, phone_number, enabled) VALUES
(1, 'admin', 'admin@zuriagro.com', '$2a$10$h88a44y8vjQjHw4H9w4XDeN3.n8G5R56D1n6W/rUuV4oE5x1b1HwG', 'System Administrator', '+254700000001', TRUE),
(2, 'depotmgr', 'manager.eldoret@zuriagro.com', '$2a$10$h88a44y8vjQjHw4H9w4XDeN3.n8G5R56D1n6W/rUuV4oE5x1b1HwG', 'Eldoret Depot Manager', '+254700000002', TRUE),
(3, 'farmer_john', 'john.kamau@farms.com', '$2a$10$h88a44y8vjQjHw4H9w4XDeN3.n8G5R56D1n6W/rUuV4oE5x1b1HwG', 'John Kamau', '+254711000003', TRUE)
ON CONFLICT (username) DO NOTHING;

-- 5. Map Users to Roles
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- admin -> ROLE_ADMIN
(1, 2), -- admin -> ROLE_DEPOT_MANAGER
(2, 2), -- depotmgr -> ROLE_DEPOT_MANAGER
(3, 3)  -- farmer_john -> ROLE_FARMER
ON CONFLICT DO NOTHING;

-- Advance sequences
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('grain_types_id_seq', (SELECT MAX(id) FROM grain_types));
SELECT setval('storage_depots_id_seq', (SELECT MAX(id) FROM storage_depots));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
