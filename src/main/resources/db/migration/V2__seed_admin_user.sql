-- V2__seed_admin_user.sql
-- Seed initial admin user for the application
-- Password: Admin@123 (BCrypt hashed)

INSERT INTO _user (firstname, lastname, email, password, role, enabled, locked, email_verified, created_date)
VALUES (
    'System',
    'Administrator',
    'admin@example.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iBT6yMk4jQD9LkZlBhLbLbLbLbLbLb',
    'ADMIN',
    true,
    false,
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- Note: The password hash above is a placeholder
-- In production, generate a proper BCrypt hash for 'Admin@123'
-- You can use: BCrypt.hashpw("Admin@123", BCrypt.gensalt(10))
