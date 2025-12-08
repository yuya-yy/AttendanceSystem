package repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.User;

/**
 * users テーブルを操作するための Repository。
 * - ユーザー名から有効ユーザーを1件取得
 * - 部署IDから有効ユーザー一覧を取得
 * などのメソッドを定義する。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * ユーザー名をキーに「有効なユーザー」を1件取得する。
     * deleted_at が null のレコードのみ対象。
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.username = :username
              AND u.deletedAt IS NULL
            """)
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * 部署IDを指定して、その部署に所属する「有効なユーザー一覧」を取得する。
     * deleted_at が null のレコードのみ対象。
     * 一覧は id 昇順で返す。
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.department.id = :departmentId
              AND u.deletedAt IS NULL
            ORDER BY u.id ASC
            """)
    List<User> findActiveByDepartmentId(@Param("departmentId") Integer departmentId);

    // ※ findById, save などの基本メソッドは JpaRepository がすでに持っているので、
    // ここで改めて定義する必要はありません。
}
