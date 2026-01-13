-- =========================================
-- 勤怠サンプルデータ
-- テーブル: attendance_records
-- しっかりと履歴が残っている人を複数用意
-- =========================================

-- 開発環境用：既存の勤怠データをいったん全削除
TRUNCATE TABLE attendance_records RESTART IDENTITY CASCADE;

-- 直近 90 日分の勤怠（ユーザー 1〜20）
INSERT INTO attendance_records (
    user_id,
    work_date,
    clock_in,
    clock_out,
    work_location_name
)
SELECT
    u AS user_id,
    d::date AS work_date,
    (d + CASE EXTRACT(DOW FROM d)::int
        WHEN 0 THEN time '10:00'
        WHEN 1 THEN time '09:00'
        WHEN 2 THEN time '08:30'
        WHEN 3 THEN time '10:00'
        WHEN 4 THEN time '09:30'
        WHEN 5 THEN time '11:00'
        WHEN 6 THEN time '09:00'
    END)::timestamptz AS clock_in,
    (d + CASE EXTRACT(DOW FROM d)::int
        WHEN 0 THEN time '18:00'
        WHEN 1 THEN time '18:00'
        WHEN 2 THEN time '17:30'
        WHEN 3 THEN time '19:00'
        WHEN 4 THEN time '18:30'
        WHEN 5 THEN time '20:00'
        WHEN 6 THEN time '16:00'
    END)::timestamptz AS clock_out,
    CASE ((u + EXTRACT(DAY FROM d)::int) % 5)
        WHEN 0 THEN '会社'
        WHEN 1 THEN '在宅'
        WHEN 2 THEN '出張'
        WHEN 3 THEN '客先'
        ELSE 'その他'
    END AS work_location_name
FROM generate_series(1, 20) AS u
CROSS JOIN generate_series(CURRENT_DATE - INTERVAL '89 days', CURRENT_DATE, INTERVAL '1 day') AS d;

-- 半日勤務パターン（ユーザー 21〜25、直近 14 日）
INSERT INTO attendance_records (
    user_id,
    work_date,
    clock_in,
    clock_out,
    work_location_name
)
SELECT
    u AS user_id,
    d::date AS work_date,
    (d + time '09:00')::timestamptz AS clock_in,
    (d + time '12:00')::timestamptz AS clock_out,
    CASE ((u + EXTRACT(DAY FROM d)::int) % 5)
        WHEN 0 THEN '会社'
        WHEN 1 THEN '在宅'
        WHEN 2 THEN '出張'
        WHEN 3 THEN '客先'
        ELSE 'その他'
    END AS work_location_name
FROM generate_series(21, 25) AS u
CROSS JOIN generate_series(CURRENT_DATE - INTERVAL '13 days', CURRENT_DATE, INTERVAL '1 day') AS d;

-- 夜勤パターン（ユーザー 2, 3 / 直近 3 回分）
INSERT INTO attendance_records (
    user_id,
    work_date,
    clock_in,
    clock_out,
    work_location_name
)
VALUES
    (2, CURRENT_DATE - INTERVAL '21 days', (CURRENT_DATE - INTERVAL '21 days') + time '20:00', (CURRENT_DATE - INTERVAL '20 days') + time '05:00', '会社'),
    (3, CURRENT_DATE - INTERVAL '14 days', (CURRENT_DATE - INTERVAL '14 days') + time '20:30', (CURRENT_DATE - INTERVAL '13 days') + time '05:30', '在宅'),
    (2, CURRENT_DATE - INTERVAL '7 days',  (CURRENT_DATE - INTERVAL '7 days') + time '21:00', (CURRENT_DATE - INTERVAL '6 days') + time '06:00', '会社');

-- created_at / updated_at / deleted_at はテーブル定義の DEFAULT に任せる
