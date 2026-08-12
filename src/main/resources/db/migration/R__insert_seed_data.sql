-- 既存データの重複挿入を防ぐため一度削除（開発・検証環境用）
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- テスト用初期データの挿入
INSERT INTO users (username, email) VALUES
('admin_user', 'admin@example.com'),
('test_user_01', 'user01@example.com'),
('test_user_02', 'user02@example.com');