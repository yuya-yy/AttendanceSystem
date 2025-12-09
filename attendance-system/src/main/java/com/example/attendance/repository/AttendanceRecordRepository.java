package com.example.attendance.repository;

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
 * save() メソッドは JpaRepositoryから継承。
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {

  // ユーザーIDを指定して、そのユーザーの最新の未退勤レコードを取得する。
  @Query("""
      SELECT attendanceRecord
      FROM AttendanceRecord attendanceRecord
      WHERE attendanceRecord.user.id = :userId
        AND attendanceRecord.clockOut IS NULL
        AND attendanceRecord.deletedAt IS NULL
      ORDER BY attendanceRecord.clockIn DESC
      """)
  Optional<AttendanceRecord> findLatestUnfinished(@Param("userId") Integer userId);

  // 部署IDを指定して、その部署に所属する未退勤の勤怠レコード一覧を取得する。
  @Query("""
      SELECT attendanceRecord
      FROM AttendanceRecord attendanceRecord
      WHERE attendanceRecord.user.department.id = :departmentId
        AND attendanceRecord.clockOut IS NULL
        AND attendanceRecord.deletedAt IS NULL
      ORDER BY attendanceRecord.clockIn ASC
      """)
  List<AttendanceRecord> findUnfinishedByDepartmentId(@Param("departmentId") Integer departmentId);

  // 指定されたユーザーIDと日付範囲に該当する勤怠レコード一覧を取得する。
  @Query("""
      SELECT attendanceRecord
      FROM AttendanceRecord attendanceRecord
      WHERE attendanceRecord.user.id = :userId
        AND attendanceRecord.workDate BETWEEN :fromDate AND :toDate
        AND attendanceRecord.deletedAt IS NULL
      ORDER BY attendanceRecord.workDate ASC, attendanceRecord.clockIn ASC
      """)
  List<AttendanceRecord> findByUserIdBetweenDates(
      @Param("userId") Integer userId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);
}
