package com.example.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.User;

/**
 * users テーブルを操作するための Repository。
 * findById(), save() メソッドは JpaRepositoryから継承。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

  // username をキーに1件のユーザーを取得する。
  @Query("""
      SELECT user
      FROM User user
      WHERE user.username = :username
        AND user.deletedAt IS NULL
      """)
  Optional<User> findByUsername(@Param("username") String username);

  // 部署IDをキーに、その部署に所属する有効なユーザー一覧を取得する。
  @Query("""
      SELECT user
      FROM User user
      WHERE user.department.id = :departmentId
        AND user.deletedAt IS NULL
      ORDER BY user.id ASC
      """)
  List<User> findActiveByDepartmentId(@Param("departmentId") Integer departmentId);

  // 有効なユーザー 一覧を取得する。
  @Query("""
      SELECT user
      FROM User user
      WHERE user.deletedAt IS NULL
      ORDER BY user.id ASC
      """)
  List<User> findAllActive();

}
