package com.example.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.Department;

/**
 * departments テーブルを操作するための Repository。
 * findById()メソッドは JpaRepositoryから継承。
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    // 有効な部署一覧を取得する。
    @Query("""
            SELECT department
            FROM Department department
            WHERE department.deletedAt IS NULL
            ORDER BY department.id ASC
            """)
    List<Department> findAllActive();

    // 有効な部署が存在するか確認する。
    boolean existsByIdAndDeletedAtIsNull(Integer id);

    // 有効な部署をIDで1件取得する。（部署名表示に使う）
    Optional<Department> findByIdAndDeletedAtIsNull(Integer id);
}
