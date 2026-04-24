-- V1__initial_schema.sql
-- Initial database schema for authentication service

-- Create users table
CREATE TABLE IF NOT EXISTS _user (
    id SERIAL PRIMARY KEY,
    firstname VARCHAR(50) NOT NULL,
    lastname VARCHAR(50) NOT NULL,
    date_of_birth DATE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT false NOT NULL,
    locked BOOLEAN DEFAULT false NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0 NOT NULL,
    account_locked_until TIMESTAMP NULL,
    email_verification_token VARCHAR(255),
    email_verified BOOLEAN DEFAULT false NOT NULL,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_user_email ON _user(email);
CREATE INDEX idx_user_role ON _user(role);
CREATE INDEX idx_user_enabled ON _user(enabled);
CREATE INDEX idx_user_email_verification_token ON _user(email_verification_token);
CREATE INDEX idx_user_password_reset_token ON _user(password_reset_token);

-- Add comment to table
COMMENT ON TABLE _user IS 'User accounts for authentication service';
COMMENT ON COLUMN _user.email IS 'Unique email address used for login';
COMMENT ON COLUMN _user.password IS 'BCrypt hashed password';
COMMENT ON COLUMN _user.role IS 'User role: USER or ADMIN';
COMMENT ON COLUMN _user.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN _user.account_locked_until IS 'Account lockout expiry time after too many failed attempts';
COMMENT ON COLUMN _user.email_verified IS 'Whether the user has verified their email address';
