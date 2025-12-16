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

  /**
   * メールアドレスをキーに「有効なユーザー」を1件取得する。
   * deleted_at が null のレコードのみ対象。
   *
   * 新規ユーザー登録／ユーザー編集で、
   * 「同じメールアドレスのユーザーが既にいないか」を調べるために使用する。
   */
  @Query("""
      SELECT u
      FROM User u
      WHERE u.email = :email
        AND u.deletedAt IS NULL
      """)
  Optional<User> findByEmail(@Param("email") String email);

  /**
   * 電話番号をキーに「有効なユーザー」を1件取得する。
   * deleted_at が null のレコードのみ対象。
   *
   * 新規ユーザー登録／ユーザー編集で、
   * 「同じ電話番号のユーザーが既にいないか」を調べるために使用する。
   */
  @Query("""
      SELECT u
      FROM User u
      WHERE u.phone = :phone
        AND u.deletedAt IS NULL
      """)
  Optional<User> findByPhone(@Param("phone") String phone);

  // 有効なユーザー 一覧を取得する。
  @Query("""
      SELECT user
      FROM User user
      WHERE user.deletedAt IS NULL
      ORDER BY user.id ASC
      """)
  List<User> findAllActive();

  Optional<User> findByIdAndDeletedAtIsNull(Integer id);
}
