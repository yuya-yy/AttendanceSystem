-- ==========================================================
-- 勤怠管理システム 用 PostgreSQL スキーマ定義
-- データベース名：attendance_system  （←これは別途作成しておく）
-- スキーマ　　：public
-- ==========================================================

-- 念のためスキーマを指定（デフォルトの public を利用）
SET search_path TO public;

-- ==========================================================
-- 部署テーブル（departments）
-- ==========================================================
CREATE TABLE departments (
    id              SERIAL          PRIMARY KEY,              -- 部署ID（自動採番）
    department_name VARCHAR(100)    NOT NULL UNIQUE,          -- 部署名（一意）
    deleted_at      TIMESTAMPTZ,                              -- 論理削除日時（未削除はNULL）
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),   -- 作成日時
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()    -- 更新日時
);

-- ==========================================================
-- 勤務場所マスタテーブル（work_locations）
-- 例：code = 'OFFICE' / 'HOME' / 'TRIP' / 'CLIENT' / 'OTHER'
-- ==========================================================
CREATE TABLE work_locations (
    id              SERIAL          PRIMARY KEY,              -- 勤務場所ID（自動採番）
    code            VARCHAR(50)     NOT NULL UNIQUE,          -- 勤務場所コード（一意）
    location_name   VARCHAR(100)    NOT NULL,                 -- 表示名（例：会社 / 在宅 / 出張）
    deleted_at      TIMESTAMPTZ,                              -- 論理削除日時
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),   -- 作成日時
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()    -- 更新日時
);

-- ==========================================================
-- ユーザーテーブル（users）
-- ログイン情報・所属部署・連絡先・権限など
-- ==========================================================
CREATE TABLE users (
    id                      SERIAL          PRIMARY KEY,              -- ユーザーID（自動採番）
    username                VARCHAR(50)     NOT NULL UNIQUE,          -- ログインID（一意）
    display_name            VARCHAR(100)    NOT NULL,                 -- 画面表示用の名前
    password_hash           TEXT            NOT NULL,                 -- パスワード（ハッシュ値）
    email                   VARCHAR(255)    UNIQUE,                   -- メールアドレス（一意・NULL許可）
    phone                   VARCHAR(20),                              -- 電話番号（NULL許可）
    role                    SMALLINT        NOT NULL DEFAULT 2        -- 権限フラグ 1=admin, 2=user
        CHECK (role IN (1, 2)),
    department_id           INTEGER         NOT NULL                  -- 所属部署ID（departments.id）
        REFERENCES departments(id),
    default_work_location_id INTEGER                                     -- 既定の勤務場所ID（work_locations.id）
        REFERENCES work_locations(id),
    deleted_at              TIMESTAMPTZ,                              -- 論理削除日時
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),   -- 作成日時
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now()    -- 更新日時
);

-- ==========================================================
-- 勤怠記録テーブル（attendance_records）
-- 出勤・退勤の実績と、その日の勤務場所名を記録する
-- ==========================================================
CREATE TABLE attendance_records (
    id                  SERIAL          PRIMARY KEY,              -- 勤怠ID（自動採番）
    user_id             INTEGER         NOT NULL                  -- ユーザーID（users.id）
        REFERENCES users(id),
    work_date           DATE            NOT NULL,                 -- 勤務日（出勤日）
    clock_in            TIMESTAMPTZ     NOT NULL,                 -- 出勤日時
    clock_out           TIMESTAMPTZ,                              -- 退勤日時（未退勤はNULL）
    work_location_name  VARCHAR(100)    NOT NULL,                 -- 当日の勤務場所名（実績をコピーして固定保存）
    deleted_at          TIMESTAMPTZ,                              -- 論理削除日時（将来拡張用）
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),   -- 作成日時
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()    -- 更新日時
);
