-- V4__update_seed_passwords.sql
-- Ensure seed users have valid BCrypt hash for 'Admin@12345'
UPDATE users 
SET password_hash = '$2a$10$WCBtsx5swRiyZYPcG0KlBepUOZuzBjOVhkP52t2w8ATugkiv6Bc6.' 
WHERE username IN ('admin', 'depotmgr', 'farmer_john');
