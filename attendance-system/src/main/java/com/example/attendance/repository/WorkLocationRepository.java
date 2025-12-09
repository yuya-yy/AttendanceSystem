package com.example.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // 勤務場所コードをキーに、1件の勤務場所マスタを取得する。//使用しない可能性あり
    Optional<WorkLocation> findByCode(String code);

    // 有効な勤務場所一覧を取得する。
    @Query("""
            SELECT workLocation
            FROM WorkLocation workLocation
            WHERE workLocation.deletedAt IS NULL
            ORDER BY workLocation.id ASC
            """)
    List<WorkLocation> findAllActive();
}
