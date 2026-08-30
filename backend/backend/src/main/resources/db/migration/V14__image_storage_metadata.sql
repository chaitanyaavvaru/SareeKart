-- V14: File-storage metadata on product images.
-- url remains for backward compatibility; storage_key is the authoritative
-- S3/local object key used for deletion and replacement.

ALTER TABLE product_images
    ADD COLUMN storage_key VARCHAR(500) NULL AFTER url,
    ADD COLUMN content_type VARCHAR(100) NULL AFTER alt_text,
    ADD COLUMN file_size BIGINT NULL AFTER content_type;

CREATE INDEX idx_product_images_storage_key ON product_images (storage_key);