package entity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*; // Entity / Column / ManyToOne などをまとめてインポート

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // 論理削除
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 作成日時・更新日時（値の設定はService側で行う想定）
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================
    // ここから「1レコードの振る舞い」
    // ============================

    /**
     * 出勤処理（新しく作成したレコードに対して呼ぶ想定）。
     *
     * @param now              サーバーの現在時刻
     * @param workLocationName 出勤時点の勤務場所名（「会社」「在宅」など）
     */
    public void startWork(OffsetDateTime now, String workLocationName) {
        if (this.clockIn != null) {
            // すでに出勤済みならおかしい
            throw new IllegalStateException("すでに出勤済みのレコードです。");
        }
        this.workDate = now.toLocalDate(); // 勤務日
        this.clockIn = now; // 出勤時刻
        this.workLocationName = workLocationName; // 勤務場所名をコピー
    }

    /**
     * 退勤処理（未退勤レコードに対して呼ぶ想定）。
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
     * 勤務中かどうか（退勤しておらず、論理削除されていない）。
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
        return Duration.between(this.clockIn, this.clockOut).toMinutes();
    }
}
