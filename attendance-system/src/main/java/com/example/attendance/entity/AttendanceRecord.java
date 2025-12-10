package com.example.attendance.entity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 勤怠記録（attendance_recordsテーブル）を表すエンティティ。
 *
 * - 1レコードが 「ユーザーの1回の勤務（出勤〜退勤）」を表す
 * - 出勤／退勤の処理ロジックも、このクラスのメソッドとして持たせる
 */
@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // users.id への外部キー（多対1）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 勤務日（出勤ボタンを押した日の「日付」）
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // 出勤時刻
    @Column(name = "clock_in", nullable = false)
    private OffsetDateTime clockIn;

    // 退勤時刻（退勤前はNULL）
    @Column(name = "clock_out")
    private OffsetDateTime clockOut;

    // 出勤時点の勤務場所名（会社／在宅／出張…）
    @Column(name = "work_location_name", nullable = false, length = 100)
    private String workLocationName;

    // 論理削除日時
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 作成日時
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 出勤処理。
     *
     * @param now              サーバーの現在時刻
     * @param workLocationName 出勤時点の勤務場所名（「会社」「在宅」など）
     */
    public void startWork(OffsetDateTime now, String workLocationName) {
        if (this.clockIn != null) {
            throw new IllegalStateException("すでに出勤済みのレコードです。");
        }
        this.workDate = now.toLocalDate(); // 勤務日
        this.clockIn = now; // 出勤時刻
        this.workLocationName = workLocationName; // 勤務場所名をコピー
    }

    /**
     * 退勤処理
     *
     * @param now サーバーの現在時刻
     */
    public void finishWork(OffsetDateTime now) {
        if (this.clockIn == null) {
            throw new IllegalStateException("出勤していないレコードは退勤できません。");
        }
        if (this.clockOut != null) {
            throw new IllegalStateException("すでに退勤済みのレコードです。");
        }
        this.clockOut = now;
    }

    /**
     * 勤務中かどうかの判定。（退勤しておらず、論理削除されていない）。
     */
    public boolean isUnfinished() {
        return this.clockOut == null && this.deletedAt == null;
    }

    /**
     * 勤務時間（分）を計算する。
     * clockIn / clockOut のどちらかが NULL の場合は 0 分とする。
     */
    public long calculateWorkingMinutes() {
        if (this.clockIn == null || this.clockOut == null) {
            return 0L;
        }

        // 表示している「時:分」に合わせるため、秒以下を切り捨て
        OffsetDateTime clockInMinutes = this.clockIn.truncatedTo(ChronoUnit.MINUTES);
        OffsetDateTime clockOutMinutes = this.clockOut.truncatedTo(ChronoUnit.MINUTES);

        return Duration.between(clockInMinutes, clockOutMinutes)
                .toMinutes();
    }

    public String getWorkingTimeText() {
        // 出勤中（退勤していない）など → 空文字で表示したい
        if (this.clockIn == null || this.clockOut == null) {
            return "";
        }

        // 分数の計算ルールは calculateWorkingMinutes() に統一
        long minutes = calculateWorkingMinutes();

        // 0分ちょうどなら「0分」
        if (minutes <= 0) {
            return "0分";
        }

        long hours = minutes / 60;
        long restMinutes = minutes % 60;

        if (hours == 0) {
            // 0時間○分 → 「○分」だけ表示
            return restMinutes + "分";
        }

        if (restMinutes == 0) {
            // ○時間0分 → 「○時間」だけ
            return hours + "時間";
        }

        // ○時間△分
        return hours + "時間" + restMinutes + "分";
    }
}
