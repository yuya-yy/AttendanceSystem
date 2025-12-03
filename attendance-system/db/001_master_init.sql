-- =========================================
-- 部署マスタ 初期データ
-- テーブル: departments
-- =========================================

-- いったん全削除（開発環境用）
TRUNCATE TABLE departments RESTART IDENTITY CASCADE;

INSERT INTO departments (id, department_name)
VALUES
    (1, '総務部'),
    (2, '開発部'),
    (3, '営業部'),
    (4, '人事部');

-- =========================================
-- 勤務場所マスタ 初期データ
-- テーブル: work_locations
-- =========================================

-- いったん全削除（開発環境用）
TRUNCATE TABLE work_locations RESTART IDENTITY CASCADE;

INSERT INTO work_locations (id, code, location_name)
VALUES
    (1, 'OFFICE', '会社'),
    (2, 'HOME',   '在宅'),
    (3, 'TRIP',   '出張'),
    (4, 'CLIENT', '客先'),
    (5, 'OTHER',  'その他');
