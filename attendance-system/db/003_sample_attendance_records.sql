-- =========================================
-- 勤怠サンプルデータ
-- テーブル: attendance_records
-- =========================================

-- 開発環境用：既存の勤怠データをいったん全削除
TRUNCATE TABLE attendance_records RESTART IDENTITY CASCADE;

INSERT INTO attendance_records (
    user_id,
    work_date,
    clock_in,
    clock_out,
    work_location_name
)
VALUES
    -- 2025-12-01 の勤務（全員 日勤）
    (2, DATE '2025-12-01', TIMESTAMPTZ '2025-12-01 09:00:00', TIMESTAMPTZ '2025-12-01 18:00:00', '会社'),
    (3, DATE '2025-12-01', TIMESTAMPTZ '2025-12-01 10:00:00', TIMESTAMPTZ '2025-12-01 19:00:00', '在宅'),
    (4, DATE '2025-12-01', TIMESTAMPTZ '2025-12-01 09:30:00', TIMESTAMPTZ '2025-12-01 18:30:00', '出張'),
    (5, DATE '2025-12-01', TIMESTAMPTZ '2025-12-01 09:00:00', TIMESTAMPTZ '2025-12-01 17:00:00', '会社'),
    (6, DATE '2025-12-01', TIMESTAMPTZ '2025-12-01 09:00:00', TIMESTAMPTZ '2025-12-01 18:00:00', '在宅'),

    -- 2025-12-02 の勤務
    (2, DATE '2025-12-02', TIMESTAMPTZ '2025-12-02 09:00:00', TIMESTAMPTZ '2025-12-02 18:00:00', '会社'),
    (3, DATE '2025-12-02', TIMESTAMPTZ '2025-12-02 09:00:00', TIMESTAMPTZ '2025-12-02 18:00:00', '会社'),
    (4, DATE '2025-12-02', TIMESTAMPTZ '2025-12-02 13:00:00', TIMESTAMPTZ '2025-12-02 18:00:00', '客先'),

    -- 2025-12-03 の勤務（夜勤パターンあり）
    -- 夜勤：2025-12-03 20:00〜 2025-12-04 05:00（work_date は出勤日）
    (2, DATE '2025-12-03', TIMESTAMPTZ '2025-12-03 20:00:00', TIMESTAMPTZ '2025-12-04 05:00:00', '会社'),

    -- 半日勤務・その他パターン
    (3, DATE '2025-12-03', TIMESTAMPTZ '2025-12-03 09:00:00', TIMESTAMPTZ '2025-12-03 12:00:00', 'その他');
    -- created_at / updated_at / deleted_at はテーブル定義の DEFAULT に任せる
