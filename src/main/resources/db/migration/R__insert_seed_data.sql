-- 既存データの重複挿入を防ぐため一度削除（開発・検証環境用）
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- テスト用初期データの挿入（パスワードは 'password' を BCrypt 暗号化したもの）
INSERT INTO users (name, email, password_hash, role) VALUES
('管理者 太郎', 'admin@example.com', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe1wg.d.XwIuG.gYj6jM1Z8iI1jWp.X6C', 'ADMIN'),
('一般 太郎', 'user01@example.com', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe1wg.d.XwIuG.gYj6jM1Z8iI1jWp.X6C', 'STAFF'),
('一般 花子', 'user02@example.com', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe1wg.d.XwIuG.gYj6jM1Z8iI1jWp.X6C', 'STAFF');
