package com.example.attendance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.departmentRepository = departmentRepository;
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
        User user = requireActiveCurrentUser(userId);

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

        User user = requireActiveCurrentUser(userId);

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

        requireActiveCurrentUser(userId); // 存在チェックだけしておく

        AttendanceRecord record = attendanceRecordRepository
                .findLatestUnfinished(userId)
                .orElseThrow(() -> new BusinessException("error.attendance.alreadyClockOut"));

        OffsetDateTime now = OffsetDateTime.now();

        // ★ ハイブリッド型：退勤処理も Entity 側に任せる
        record.finishWork(now);
        record.setUpdatedAt(now);

        attendanceRecordRepository.save(record);
    }

    /**
     * 勤怠状況一覧画面に渡すデータをまとめる “箱”（Service専用）
     * - users：表示用ユーザー一覧（並び順込み）
     * - workingUserIds：勤務中判定用ユーザーID集合
     */
    public static record DepartmentStatusView(List<User> users, Set<Integer> workingUserIds) {
        public boolean isWorking(Integer userId) {
            return workingUserIds != null && workingUserIds.contains(userId);
        }
    }

    /**
     * 部署の勤怠状況（表示用）を取得する。
     * 「勤務中 → 勤務外 → ID昇順」に並び替えた users と、workingUserIds をまとめて返す。
     */
    @Transactional(readOnly = true)
    public DepartmentStatusView getDepartmentCurrentStatus(Integer departmentId) {

        // 部署の存在チェック
        if (!departmentRepository.existsByIdAndDeletedAtIsNull(departmentId)) {
            throw new BusinessException("error.user.department.notFound");
        }

        // ① 部署の有効ユーザー一覧（DBアクセス）
        List<User> users = userRepository.findActiveByDepartmentId(departmentId);

        // ② 勤務中ユーザーIDの Set（DBアクセス）
        Set<Integer> workingUserIds = getWorkingUserIdsInDepartment(departmentId);

        // ③ 勤務中 → 勤務外 → ID昇順 にソート
        users.sort(
                Comparator
                        .comparing((User user) -> workingUserIds.contains(user.getId()))
                        .reversed()
                        .thenComparing(User::getId));

        // ④ “箱”に詰めて返す
        return new DepartmentStatusView(users, workingUserIds);

    }

    /**
     * 指定部署で「勤務中（未退勤）」のユーザーID一覧を取得する（内部用）。
     * attendance_records.clock_out IS NULL のレコードを対象。
     *
     * ※ Controller から直接呼ばせないため internal にしている
     */
    private Set<Integer> getWorkingUserIdsInDepartment(Integer departmentId) {

        List<AttendanceRecord> unfinishedList = attendanceRecordRepository.findUnfinishedByDepartmentId(departmentId);

        return unfinishedList.stream()
                .map(AttendanceRecord::getUser)
                .filter(Objects::nonNull) // 念のため null チェック
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    // 共通：有効なユーザーであることを確認して取得する。
    public User requireActiveCurrentUser(Integer userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException("error.auth.userDeleted"));
    }
}
