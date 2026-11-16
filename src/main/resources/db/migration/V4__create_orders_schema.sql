-- =====================================================================
-- 受注ヘッダー (orders) および 受注明細 (order_items) テーブルの作成
-- =====================================================================

-- 1. orders（受注ヘッダー）テーブル
CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        order_code VARCHAR(50) NOT NULL UNIQUE,
                        customer_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'ORDERED',
                        total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                        ordered_at TIMESTAMP NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外部キー制約
                        CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
                        CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- インデックス作成（検索パフォーマンス向上用）
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);

-- 2. order_items（受注明細）テーブル
CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10, 2) NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外部キー制約
                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- インデックス作成
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);