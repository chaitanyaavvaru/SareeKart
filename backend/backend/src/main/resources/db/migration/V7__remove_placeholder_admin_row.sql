-- V7: Remove the placeholder admin row from V3.
-- Its password hash was authored outside of any real BCrypt encoder and can
-- never authenticate. The application's DataInitializer recreates this
-- account at startup using the live PasswordEncoder, guaranteeing the
-- documented credentials (admin@sareekart.com / admin123) actually work.
-- user_roles rows disappear via ON DELETE CASCADE.

DELETE FROM users WHERE email = 'admin@sareekart.com';