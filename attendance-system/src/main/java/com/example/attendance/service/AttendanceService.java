package com.example.attendance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.UserRepository;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
            UserRepository userRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.userRepository = userRepository;
    }

    /**
     * 勤怠入力画面用：最新の未退勤レコードを取得する。
     * （勤務中かどうかの判定に使う）
     */
    @Transactional(readOnly = true)
    public AttendanceRecord findLatestUnfinished(Integer userId) {
        return attendanceRecordRepository
                .findLatestUnfinished(userId)
                .orElse(null);
    }

    /**
     * 勤怠入力画面用：直近30日分の勤怠履歴を取得する。
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecord> getRecentRecords(Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(29); // 当日を含めて30日分

        return attendanceRecordRepository.findByUserIdBetweenDates(userId, from, today);
    }

    /**
     * 現在の「勤務場所名」を取得する。
     * users.default_work_location_id → work_locations.location_name を使う。
     * 未設定の場合は「未設定」を返す（画面でそのまま表示してOK）。
     */
    @Transactional(readOnly = true)
    public String getCurrentWorkLocationName(Integer userId) {

        // ユーザー取得（存在しないのはシステム的におかしいので例外）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("error.auth.loginFailed"));

        // デフォルト勤務場所を参照
        WorkLocation defaultWorkLocation = user.getDefaultWorkLocation();

        if (defaultWorkLocation == null) {
            // 勤務場所がまだ設定されていない場合
            return "未設定";
        }

        // 勤務場所マスタの名前（例：会社／在宅／出張／客先／その他）
        return defaultWorkLocation.getLocationName();
    }

    /**
     * 出勤処理。
     * - すでに未退勤レコードがある場合はエラー（出勤ボタンを押せない状態）
     * - なければ新しい AttendanceRecord を作り、Entity に出勤処理させて保存
     */
    @Transactional
    public void recordClockIn(Integer userId) {

        User user = loadActiveUser(userId);

        // すでに勤務中ならエラー
        Optional<AttendanceRecord> unfinishedOpt = attendanceRecordRepository.findLatestUnfinished(userId);

        if (unfinishedOpt.isPresent()) {
            throw new BusinessException("error.attendance.alreadyClockIn");
        }

        // 現在の勤務場所名（未設定なら「未設定」）
        String workLocationName = getCurrentWorkLocationName(userId);

        OffsetDateTime now = OffsetDateTime.now();

        AttendanceRecord record = new AttendanceRecord();
        record.setUser(user);
        record.setDeletedAt(null);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        // ★ ハイブリッド型：出勤ロジックは Entity のメソッドに任せる
        record.startWork(now, workLocationName);

        attendanceRecordRepository.save(record);
    }

    /**
     * 退勤処理。
     * - 未退勤レコードが無い場合は「すでに退勤済み」とみなしてエラー
     * - 見つかったレコードに対して Entity の退勤処理を呼び出す
     */
    @Transactional
    public void recordClockOut(Integer userId) {

        loadActiveUser(userId); // 存在チェックだけしておく

        AttendanceRecord record = attendanceRecordRepository
                .findLatestUnfinished(userId)
                .orElseThrow(() -> new BusinessException("error.attendance.alreadyClockOut"));

        OffsetDateTime now = OffsetDateTime.now();

        // ★ ハイブリッド型：退勤処理も Entity 側に任せる
        record.finishWork(now);
        record.setUpdatedAt(now);

        attendanceRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveUsersInDepartment(Integer departmentId) {
        return userRepository.findActiveByDepartmentId(departmentId);
    }

    /**
     * 指定部署で「勤務中（未退勤）」のユーザーID一覧を取得する。
     * attendance_records.clock_out IS NULL となっているレコードを対象にする。
     */
    @Transactional(readOnly = true)
    public Set<Integer> getWorkingUserIdsInDepartment(Integer departmentId) {

        // 部署内の未退勤レコード一覧を取得
        List<AttendanceRecord> unfinishedList = attendanceRecordRepository.findUnfinishedByDepartmentId(departmentId);

        // user_id の Set に変換（勤務中ユーザーのID一覧）
        return unfinishedList.stream()
                .map(record -> record.getUser().getId())
                .collect(Collectors.toSet());
    }

    // ===== 内部ヘルパー =====

    /**
     * 有効なユーザーを取得する。
     * 論理削除済みなら BusinessException を投げる。
     */
    private User loadActiveUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("error.auth.userDeleted"));

        if (!user.isActive()) {
            throw new BusinessException("error.auth.userDeleted");
        }
        return user;
    }

    public User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("error.common.invalidParameter"));
    }
}
