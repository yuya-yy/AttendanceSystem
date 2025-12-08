package com.example.attendance.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 部署マスタ（departments テーブル）に対応するエンティティ。
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    // 主キー
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 部署名（ユニーク）
    @Column(name = "department_name", nullable = false, unique = true, length = 100)
    private String departmentName;

    // 論理削除日時（削除されていない場合は null）
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 作成日時
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ====== 便利メソッド（DBカラムにはしない） ======

    /**
     * 有効なレコードかどうか（論理削除されていないか）。
     */
    @Transient
    public boolean isActive() {
        return deletedAt == null;
    }
}
