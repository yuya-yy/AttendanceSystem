package com.example.attendance.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 勤務場所マスタ（work_locations テーブル）に対応するエンティティ。
 * 会社／在宅／出張／客先／その他 などを管理する。
 */
@Entity
@Table(name = "work_locations")
@Getter
@Setter
@NoArgsConstructor
public class WorkLocation {

    public static final String CODE_OFFICE = "OFFICE";
    public static final String CODE_HOME = "HOME";
    public static final String CODE_TRIP = "TRIP";
    public static final String CODE_CLIENT = "CLIENT";
    public static final String CODE_OTHER = "OTHER";

    // 主キー
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 勤務場所コード（OFFICE/HOME/TRIP/CLIENT/OTHER）
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    // 勤務場所名（会社／在宅／出張／客先／その他）
    @Column(name = "location_name", nullable = false, length = 100)
    private String locationName;

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
