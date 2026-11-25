-- =====================================================================
-- 入出庫履歴ログ (stock_histories) テーブルの作成
-- =====================================================================
CREATE TABLE stock_histories (
                                 id BIGSERIAL PRIMARY KEY,
                                 product_id BIGINT NOT NULL,
                                 user_id BIGINT,
                                 change_type VARCHAR(20) NOT NULL, -- INBOUND（入荷）, OUTBOUND（出荷）, ADJUSTMENT（調整）
                                 quantity_change INT NOT NULL, -- 増減数（+10, -5 など）
                                 reason VARCHAR(255), -- 変動理由・関連伝票番号
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 CONSTRAINT fk_stock_histories_product FOREIGN KEY (product_id) REFERENCES products(id),
                                 CONSTRAINT fk_stock_histories_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_stock_histories_product_id ON stock_histories(product_id);
CREATE INDEX idx_stock_histories_user_id ON stock_histories(user_id);
CREATE INDEX idx_stock_histories_change_type ON stock_histories(change_type);