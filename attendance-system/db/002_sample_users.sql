-- =========================================
-- ユーザーサンプルデータ
-- テーブル: users
-- 開発環境用のダミーデータ
-- =========================================

-- いったん全削除（開発環境用）
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

INSERT INTO users (
    id,
    username,
    display_name,
    password_hash,
    email,
    phone,
    role,
    department_id,
    default_work_location_id
)
VALUES
    -- 管理者ユーザー（role = 1）
    (1, 'admin1', '管理者 太郎', 'adminpass', 'admin1@example.com', '090-1111-1111', 1, 2, 1),

    -- 開発部メンバー（department_id = 2）
    (2, 'dev_yamada', '山田 太郎', 'password', 'yamada.dev@example.com', '080-2222-2222', 2, 2, 1),
    (3, 'dev_suzuki', '鈴木 花子', 'password', 'suzuki.dev@example.com', '080-3333-3333', 2, 2, 2),

    -- 営業部メンバー（department_id = 3）
    (4, 'sales_tanaka', '田中 一郎', 'password', 'tanaka.sales@example.com', '080-4444-4444', 2, 3, 3),

    -- 総務部メンバー（department_id = 1）
    (5, 'general_sato', '佐藤 次郎', 'password', 'sato.general@example.com', NULL, 2, 1, 1),

    -- 人事部メンバー（department_id = 4）
    (6, 'hr_yoshida', '吉田 三郎', 'password', 'yoshida.hr@example.com', '080-6666-6666', 2, 4, 2);

-- created_at / updated_at はテーブル定義の DEFAULT now() に任せる想定
-- deleted_at は全員 NULL（有効ユーザー）のまま
