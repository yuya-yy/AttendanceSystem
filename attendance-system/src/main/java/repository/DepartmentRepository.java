package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.Department;

/**
 * departments テーブルを操作するための Repository。
 *
 * - 有効な部署一覧（deleted_at IS NULL）を取得する
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * 有効な部署マスタ一覧を取得する。
     *
     * 条件：
     * - deletedAt IS NULL（論理削除されていない）
     *
     * 並び順：
     * - id 昇順
     */
    @Query("""
            SELECT d
            FROM Department d
            WHERE d.deletedAt IS NULL
            ORDER BY d.id ASC
            """)
    List<Department> findAllActive();
}
