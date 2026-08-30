-- V6: Align reviews.rating column type with the entity (Integer -> INTEGER).
-- TINYINT caused Hibernate schema validation to fail.

ALTER TABLE reviews MODIFY COLUMN rating INT NOT NULL;