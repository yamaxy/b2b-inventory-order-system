# B2B Inventory & Order Management System (b2b-inventory-order-system)

B2B（企業間取引）向けの在庫・注文管理システム バックエンドAPIアプリケーションです。

---

## 🛠 技術スタック

| カテゴリ               | 技術 / ツール     |
|:-----------------------|:------------------|
| **言語**               | Java 21           |
| **フレームワーク**     | Spring Boot 4.0.7 |
| **データベース**       | PostgreSQL 16     |
| **DBマイグレーション** | Flyway            |
| **コンテナ環境**       | Docker            |
| **ビルドツール**       | Gradle            |
| **IDE        **        | IntelliJ IDEA     |
---

### 📌 フェーズ進捗状況
- [x] **P1: 要件定義・基本設計**
- [x] **P2: 開発環境構築・DB設計**
- [x] **P3: バックエンド開発（基本CRUD・認証）**
    - [x] P3-1: パッケージ構造定義・共通例外ハンドリングの実装
    - [x] P3-2: ユーザー認証機能の実装（Spring Security / JWT等）
    - [x] P3-3: ユーザー管理 API（CRUD）の実装
    - [x] P3-4: 商品マスター管理 API（CRUD）の実装
    - [x] P3-5: 取引先マスター管理 API（CRUD）の実装
    - [x] P3-6: APIテストの作成・P3動作確認 & ドキュメント更新
- [ ] **P4: 受注・在庫制御ロジック（コア機能）**

---

### 📖 API 仕様書 (Swagger UI)
アプリケーション起動後、以下の URL から API 仕様の確認およびテスト実行が可能です。
- **URL**: `http://localhost:8080/swagger-ui/index.html`

