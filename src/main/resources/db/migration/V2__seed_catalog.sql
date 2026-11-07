INSERT INTO categories (name, description)
VALUES
    ('Books', 'Printed and digital books'),
    ('Electronics', 'Devices, accessories, and computing')
;

INSERT INTO categories (name, description, parent_id)
SELECT 'Computers', 'Computers and related accessories', id
FROM categories
WHERE name = 'Electronics'
;

INSERT INTO products (sku, name, description, price, inventory_quantity, active)
VALUES
    ('BOOK-001', 'Sample Book', 'A sample catalog book.', 19.99, 50, TRUE),
    ('ELEC-001', 'Sample Headphones', 'A sample catalog accessory.', 49.99, 25, TRUE),
    ('COMP-001', 'Sample Keyboard', 'A sample computer accessory.', 39.99, 30, TRUE)
;

INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id
FROM products p
JOIN categories c ON c.name = CASE p.sku
    WHEN 'BOOK-001' THEN 'Books'
    WHEN 'ELEC-001' THEN 'Electronics'
    WHEN 'COMP-001' THEN 'Computers'
END
WHERE p.sku IN ('BOOK-001', 'ELEC-001', 'COMP-001')
;
