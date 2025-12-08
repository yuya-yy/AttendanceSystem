package repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.AttendanceRecord;

/**
 * attendance_records テーブルを操作するための Repository。
 *
 * - 指定ユーザーの最新の未退勤レコードを取得する
 * - 部署ごとの未退勤レコード一覧を取得する
 * - ユーザーの期間内勤怠一覧を取得する
 * などのメソッドを定義する。
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {

    /**
     * 指定ユーザーについて、最新の未退勤レコードを1件取得する。
     *
     * 条件：
     * - user.id = :userId
     * - clockOut IS NULL（未退勤）
     * - deletedAt IS NULL（論理削除されていない）
     *
     * 並び順：
     * - clockIn の降順（最新の出勤が先頭）
     */
    @Query("""
            SELECT ar
            FROM AttendanceRecord ar
            WHERE ar.user.id = :userId
              AND ar.clockOut IS NULL
              AND ar.deletedAt IS NULL
            ORDER BY ar.clockIn DESC
            """)
    Optional<AttendanceRecord> findLatestUnfinished(@Param("userId") Integer userId);

    /**
     * 部署IDを指定して、その部署に所属するユーザーの
     * 未退勤レコード一覧を取得する。
     *
     * 条件：
     * - user.department.id = :departmentId
     * - clockOut IS NULL（勤務中）
     * - deletedAt IS NULL（論理削除されていない）
     *
     * 並び順：
     * - clockIn の昇順（早く出勤した人から表示）
     */
    @Query("""
            SELECT ar
            FROM AttendanceRecord ar
            WHERE ar.user.department.id = :departmentId
              AND ar.clockOut IS NULL
              AND ar.deletedAt IS NULL
            ORDER BY ar.clockIn ASC
            """)
    List<AttendanceRecord> findUnfinishedByDepartmentId(@Param("departmentId") Integer departmentId);

    /**
     * ユーザーIDと日付範囲を指定して、その期間内の勤怠レコード一覧を取得する。
     *
     * 条件：
     * - user.id = :userId
     * - workDate BETWEEN :fromDate AND :toDate
     * - deletedAt IS NULL（論理削除されていない）
     *
     * 並び順：
     * - workDate 昇順 → 同じ日の中では clockIn 昇順
     */
    @Query("""
            SELECT ar
            FROM AttendanceRecord ar
            WHERE ar.user.id = :userId
              AND ar.workDate BETWEEN :fromDate AND :toDate
              AND ar.deletedAt IS NULL
            ORDER BY ar.workDate ASC, ar.clockIn ASC
            """)
    List<AttendanceRecord> findByUserIdBetweenDates(
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
