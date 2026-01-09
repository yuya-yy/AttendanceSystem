-- ==========================================================
-- 部署数上限（有効部署10件まで）を守るためのトリガ
-- 前提: departments.deleted_at が NULL のものを「有効」とみなす
-- ==========================================================

CREATE OR REPLACE FUNCTION enforce_department_limit()
RETURNS trigger AS $$
BEGIN
    IF (SELECT COUNT(*) FROM departments WHERE deleted_at IS NULL) >= 10 THEN
        RAISE EXCEPTION 'department limit exceeded (max 10)';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_department_limit_insert ON departments;
DROP TRIGGER IF EXISTS trg_department_limit_update ON departments;

CREATE TRIGGER trg_department_limit_insert
BEFORE INSERT ON departments
FOR EACH ROW
EXECUTE FUNCTION enforce_department_limit();

-- 論理削除から復活させる更新にも制限をかける
CREATE TRIGGER trg_department_limit_update
BEFORE UPDATE ON departments
FOR EACH ROW
WHEN (OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL)
EXECUTE FUNCTION enforce_department_limit();
