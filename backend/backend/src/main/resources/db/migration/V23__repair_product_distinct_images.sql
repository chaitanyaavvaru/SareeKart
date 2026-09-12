-- =========================================================================
-- V23: Repair Product-Specific Image Associations & Eliminate Duplicate Fallbacks
-- =========================================================================

-- Clean up contaminated duplicate/test images for affected products
DELETE FROM product_images WHERE product_id IN (1, 12, 13, 14, 15, 16, 17);

-- Re-assign distinct, authentic handloom imagery to each product
INSERT INTO product_images (product_id, image_order, image_url) VALUES
(1, 0, 'https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),
(12, 0, 'https://images.unsplash.com/photo-1617627143233-46b92015e905?auto=format&fit=crop&fm=webp&w=800&q=80'),
(13, 0, 'https://kankatala.com/cdn/shop/files/1216423500_1.webp?v=1780133134'),
(14, 0, 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80'),
(15, 0, 'https://images.unsplash.com/photo-1583391265517-35bbdad01209?auto=format&fit=crop&fm=webp&w=800&q=80'),
(16, 0, 'https://kankatala.com/cdn/shop/files/1216730670_1.webp?v=1786097422&width=1070'),
(17, 0, 'https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=800&q=80');

