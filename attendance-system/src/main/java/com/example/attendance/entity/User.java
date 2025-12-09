package com.example.attendance.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor

/**
 * ユーザー（usersテーブル）を表すエンティティ。
 *
 * - ログインID・表示名・連絡先・権限などを管理する
 * - 所属部署（Department）やデフォルト勤務場所（WorkLocation）と関連を持つ
 */
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ログインID ※一意
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    // 表示名
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    // パスワードハッシュ
    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    // メールアドレス（任意）※一意
    @Column(name = "email", unique = true, length = 255)
    private String email;

    // 電話番号（任意）
    @Column(name = "phone", length = 20)
    private String phone;

    // 権限フラグ（1=管理者, 2=一般ユーザー）
    @Column(name = "role", nullable = false)
    private Integer role;

    // 所属部署（users.department_id → departments.id）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // デフォルト勤務場所（users.default_work_location_id → work_locations.id）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_work_location_id")
    private WorkLocation defaultWorkLocation;

    // 論理削除日時
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 作成日時
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 有効なユーザーかどうか（論理削除されていないか）。
     */
    public boolean isActive() {
        return deletedAt == null;
    }

    /**
     * 管理者ユーザーかどうか。
     * role が 1 のときを管理者とみなす。
     */
    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
