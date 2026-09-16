CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(20),
    address VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- テスト・確認用シードデータ
INSERT INTO customers (customer_code, name, phone_number, address)
VALUES
    ('CUST-001', '株式会社ABCインダストリー', '03-1234-5678', '東京都千代田区1-1-1'),
    ('CUST-002', 'XYZ商事株式会社', '06-9876-5432', '大阪府大阪市中央区2-2-2');