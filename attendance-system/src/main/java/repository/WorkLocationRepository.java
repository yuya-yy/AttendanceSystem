package repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.WorkLocation;

/**
 * work_locations テーブルを操作するための Repository。
 *
 * - 勤務場所コード（OFFICE/HOME/TRIP/CLIENT/OTHER）で1件取得する
 * - 有効な勤務場所一覧（deleted_at IS NULL）を取得する
 */
@Repository
public interface WorkLocationRepository extends JpaRepository<WorkLocation, Integer> {

    /**
     * 勤務場所コードをキーに、1件の勤務場所マスタを取得する。
     * 例：OFFICE / HOME / TRIP / CLIENT / OTHER
     *
     * （deleted_at の条件は付けていないので、
     * 必要であれば Service 側で isActive() を見て判定してください）
     */
    Optional<WorkLocation> findByCode(String code);

    /**
     * 有効な勤務場所マスタ一覧を取得する。
     *
     * 条件：
     * - deletedAt IS NULL（論理削除されていない）
     *
     * 並び順：
     * - id 昇順
     */
    @Query("""
            SELECT w
            FROM WorkLocation w
            WHERE w.deletedAt IS NULL
            ORDER BY w.id ASC
            """)
    List<WorkLocation> findAllActive();
}
