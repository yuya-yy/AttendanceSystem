package com.example.attendance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

/**
 * 管理者によるユーザー管理を担当する Service。
 * ここでは「新規ユーザー登録」の処理と、
 * 画面用のマスタ一覧取得（部署・勤務場所）を提供する。
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkLocationRepository workLocationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_USERS_PER_DEPARTMENT = 50;

    public AdminUserService(UserRepository userRepository,
            DepartmentRepository departmentRepository,
            WorkLocationRepository workLocationRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.workLocationRepository = workLocationRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 画面のプルダウン用：有効な部署一覧を取得する。
     */
    @Transactional(readOnly = true)
    public List<Department> getActiveDepartments() {
        return departmentRepository.findAllActive();
    }

    /**
     * 画面のプルダウン用：有効な勤務場所一覧を取得する。
     */
    @Transactional(readOnly = true)
    public List<WorkLocation> getActiveWorkLocations() {
        return workLocationRepository.findAllActive();
    }

    /**
     * ユーザー一覧を取得する
     *
     */
    public List<User> findAllUsers() {
        return userRepository.findAllActive();
    }

    /**
     * 新規ユーザーを登録する。
     *
     * @param username              ユーザー名（ログインID）
     * @param displayName           氏名（表示名）
     * @param rawPassword           平文パスワード
     * @param email                 メールアドレス（任意）
     * @param phone                 電話番号（任意）
     * @param role                  権限（1=管理者, 2=一般）
     * @param departmentId          所属部署ID
     * @param defaultWorkLocationId デフォルト勤務場所ID（null 許可）
     */
    @Transactional
    public void registerUser(String username,
            String displayName,
            String rawPassword,
            String email,
            String phone,
            Integer role,
            Integer departmentId,
            Integer defaultWorkLocationId) {

        // ===== 0) 前処理：トリム・空欄→null =====
        String trimmedUsername = trimOrNull(username);
        String trimmedDisplayName = trimOrNull(displayName);

        // 任意項目は「空欄ならnull」に統一（UNIQUEや重複判定が安定します）
        String trimmedEmail = trimToNull(email);
        String trimmedPhone = trimToNull(phone);
        String trimmedPassword = trimToNull(rawPassword);

        // ===== 0-1) 文字数チェック（DB定義＋確定ルール）=====
        validateUserInputLength(trimmedUsername, trimmedDisplayName, trimmedEmail, trimmedPhone, trimmedPassword);

        // ===== 1) 必須項目の簡易チェック =====
        if (trimmedUsername == null || trimmedUsername.isBlank()) {
            // ユーザー名必須（ログインと共通のメッセージ）
            throw new BusinessException("error.auth.username.required");
        }
        if (trimmedDisplayName == null || trimmedDisplayName.isBlank()) {
            // 氏名必須（ユーザー登録・編集 共通）
            throw new BusinessException("validation.displayName.required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            // パスワード必須（ログインと共通のメッセージ）
            throw new BusinessException("error.auth.password.required");
        }
        if (departmentId == null) {
            // 所属部署必須
            throw new BusinessException("validation.department.required");
        }
        if (role == null) {
            // 権限必須
            throw new BusinessException("validation.role.required");
        }

        // 部署あたりの人数上限チェック
        if (userRepository.countActiveByDepartmentId(departmentId) >= MAX_USERS_PER_DEPARTMENT) {
            throw new BusinessException("validation.department.capacity");
        }

        // ===== 2) 形式チェック =====

        // 2-1) ユーザー名：半角英数字のみ
        if (!trimmedUsername.matches("^[0-9A-Za-z]+$")) {
            throw new BusinessException("validation.username.alnum");
        }

        // 2-2) パスワード：半角英数字 + 記号のみ
        String passwordPattern = "^[0-9A-Za-z!-/:-@\\[-`{-~]+$";
        if (!rawPassword.matches(passwordPattern)) {
            throw new BusinessException("validation.password.alnumSymbol");
        }

        // 2-3) メールアドレス形式（任意入力：入っている場合だけチェック）
        if (trimmedEmail != null && !trimmedEmail.isBlank()) {
            String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (!trimmedEmail.matches(emailPattern)) {
                throw new BusinessException("validation.email.format");
            }
        }

        // 2-4) 電話番号形式（任意入力：入っている場合だけチェック）
        // 入力があれば「ハイフン削除 → 数字だけかチェック → 数字だけを保存」
        String normalizedPhone = null;
        if (trimmedPhone != null && !trimmedPhone.isBlank()) {
            String digitsOnly = trimmedPhone.replaceAll("-", "");
            if (!digitsOnly.matches("^[0-9]+$")) {
                throw new BusinessException("validation.phone.numeric");
            }
            normalizedPhone = digitsOnly;
        }

        // ===== 3) ユーザー名・メール重複チェック =====

        // ユーザー名重複
        userRepository.findByUsername(trimmedUsername).ifPresent(existing -> {
            // メッセージIDは「validation.username.duplicate」を使用
            throw new BusinessException("validation.username.duplicate");
        });

        // メールアドレス重複（入力されている場合のみ）
        if (trimmedEmail != null && !trimmedEmail.isBlank()) {
            userRepository.findByEmail(trimmedEmail).ifPresent(existing -> {
                throw new BusinessException("error.user.email.duplicate");
            });
        }

        if (normalizedPhone != null) {
            userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
                throw new BusinessException("error.user.phone.duplicate");
            });
        }

        // ===== 4) 所属部署マスタの取得 =====
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("error.user.department.notFound"));

        // ===== 5) デフォルト勤務場所マスタの取得（null 許可） =====
        WorkLocation defaultWorkLocation = null;
        if (defaultWorkLocationId != null) {
            defaultWorkLocation = workLocationRepository.findById(defaultWorkLocationId)
                    .orElseThrow(() -> new BusinessException("error.user.workLocation.notFound"));
        }

        // ===== 6) メールの「空文字 → null」正規化 =====
        String normalizedEmail = (trimmedEmail == null || trimmedEmail.isBlank()) ? null : trimmedEmail;

        // ===== 7) パスワードをハッシュ化 =====
        String passwordHash = passwordEncoder.encode(rawPassword);

        // ===== 8) User エンティティを作成 =====
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Tokyo"));

        User user = new User();
        user.setUsername(trimmedUsername);
        user.setDisplayName(trimmedDisplayName);
        user.setPasswordHash(passwordHash);
        user.setEmail(normalizedEmail); // ★ 空欄なら DB では NULL
        user.setPhone(normalizedPhone); // ★ 空欄なら NULL、入力ありなら「数字だけ」
        user.setRole(role);
        user.setDepartment(department);
        user.setDefaultWorkLocation(defaultWorkLocation);
        user.setDeletedAt(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // ===== 9) 保存 =====
        userRepository.save(user);
    }

    /**
     * * 編集対象ユーザーの詳細を取得する。
     * - 論理削除済みの場合はエラー。
     */
    @Transactional(readOnly = true)
    public User findUserDetail(Integer userId) {
        return requireActiveTargetUser(userId);
    }

    /**
     * 有効な部署一覧を取得する（プルダウン用）。
     */
    @Transactional(readOnly = true)
    public List<Department> findAllActiveDepartments() {
        return departmentRepository.findAllActive();
    }

    /**
     * ユーザー情報を更新する。
     *
     * @param targetUserId 更新対象のユーザーID
     * @param displayName  表示名
     * @param email        メールアドレス
     * @param phone        電話番号
     * @param departmentId 所属部署ID
     * @param role         権限（1=管理者, 2=一般）
     * @param rawPassword  平文パスワード（空欄なら変更しない）
     */
    @Transactional
    public void updateUser(Integer targetUserId,
            String username,
            String displayName,
            String email,
            String phone,
            Integer departmentId,
            Integer role,
            String rawPassword) {

        // ===== 0) 前処理：トリム・空欄→null =====
        String trimmedUsername = trimOrNull(username);
        String trimmedDisplayName = trimOrNull(displayName);

        // 任意項目は「空欄ならnull」に統一（UNIQUEや重複判定が安定します）
        String trimmedEmail = trimToNull(email);
        String trimmedPhone = trimToNull(phone);
        String trimmedPassword = trimToNull(rawPassword);

        // ===== 0-1) 文字数チェック（DB定義＋確定ルール）=====
        validateUserInputLength(trimmedUsername, trimmedDisplayName, trimmedEmail, trimmedPhone, trimmedPassword);

        // ===== 1) 必須チェック =====

        // 1-0) ユーザー名 必須
        if (trimmedUsername == null || trimmedUsername.isBlank()) {
            // すでに messages.properties にあるキーを再利用
            throw new BusinessException("error.auth.username.required");
        }

        // 1-1) 氏名（表示名）必須
        if (trimmedDisplayName == null || trimmedDisplayName.isBlank()) {
            throw new BusinessException("validation.displayName.required");
        }

        // 1-2) 所属部署必須
        if (departmentId == null) {
            throw new BusinessException("validation.department.required");
        }

        // 1-3) 権限必須
        if (role == null) {
            throw new BusinessException("validation.role.required");
        }

        // ===== 2) 形式チェック =====

        // 2-0) ユーザー名：半角英数字のみ
        // ^ と $ は「全文」、[] の中は許可する文字、+ は1文字以上
        if (!trimmedUsername.matches("^[0-9A-Za-z]+$")) {
            throw new BusinessException("validation.username.alnum");
        }

        // 2-1) パスワード：入力されているときだけ形式チェック
        if (trimmedPassword != null && !trimmedPassword.isBlank()) {
            String passwordPattern = "^[0-9A-Za-z!-/:-@\\[-`{-~]+$";
            if (!trimmedPassword.matches(passwordPattern)) {
                throw new BusinessException("validation.password.alnumSymbol");
            }
        }

        // 2-2) メールアドレス形式（任意入力：入っている場合だけチェック）
        if (trimmedEmail != null && !trimmedEmail.isBlank()) {
            String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (!trimmedEmail.matches(emailPattern)) {
                throw new BusinessException("validation.email.format");
            }
        }

        // 2-3) 電話番号形式（任意入力：入っている場合だけチェック）
        String normalizedPhone = null;
        if (trimmedPhone != null && !trimmedPhone.isBlank()) {
            // 「090-1234-5678」みたいな形式を想定 → ハイフンを全部削除
            String digitsOnly = trimmedPhone.replaceAll("-", "");
            if (!digitsOnly.matches("^[0-9]+$")) {
                throw new BusinessException("validation.phone.numeric");
            }
            normalizedPhone = digitsOnly; // DB には数字だけで保存
        }

        // ===== 3) 対象ユーザー取得 =====
        User user = requireActiveTargetUser(targetUserId);

        // 部署あたりの人数上限チェック（自分は除外）
        if (userRepository.countActiveByDepartmentIdExcludingUser(departmentId,
                targetUserId) >= MAX_USERS_PER_DEPARTMENT) {
            throw new BusinessException("validation.department.capacity");
        }

        // ===== 4) ユーザー名・メール重複チェック（自分以外） =====

        // 4-1) ユーザー名重複チェック
        userRepository.findByUsername(trimmedUsername).ifPresent(existing -> {
            // 自分自身以外で同じユーザー名がいたらエラー
            if (!existing.getId().equals(targetUserId)) {
                throw new BusinessException("validation.username.duplicate");
            }
        });

        // 4-2) メールアドレス重複チェック（入力されている場合のみ）
        if (trimmedEmail != null && !trimmedEmail.isBlank()) {
            userRepository.findByEmail(trimmedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(targetUserId)) {
                    throw new BusinessException("error.user.email.duplicate");
                }
            });
        }

        if (normalizedPhone != null) {
            userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
                if (!existing.getId().equals(targetUserId)) {
                    throw new BusinessException("error.user.phone.duplicate");
                }
            });
        }

        // ===== 5) 部署エンティティ取得 =====
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("error.user.department.notFound"));

        // ===== 6) エンティティへの反映 =====

        // ★ ユーザー名を更新（今回は編集可にする）
        user.setUsername(trimmedUsername);

        user.setDisplayName(trimmedDisplayName);
        user.setDepartment(department);
        user.setRole(role);

        // メール（空欄なら null）
        String newEmail = null;
        if (trimmedEmail != null && !trimmedEmail.isBlank()) {
            newEmail = trimmedEmail;
        }
        user.setEmail(newEmail);

        // 電話番号（数字のみ or null）
        user.setPhone(normalizedPhone);

        // パスワード：入力されているときだけ更新（ハッシュ化）
        if (trimmedPassword != null && !trimmedPassword.isBlank()) {
            String passwordHash = passwordEncoder.encode(trimmedPassword);
            user.setPasswordHash(passwordHash);
        }

        userRepository.save(user);
    }

    /***
     * 月別の勤務時間サマリ一覧を取得する。**対象ユーザーについて、「当月を含めた直近12か月分」の勤怠レコードを集計し、*月ごとの合計勤務時間（分）
     *
     * を YearMonth 単位で返す。**
     *
     * @param
     * targetUserId        勤怠実績を確認したい対象ユーザーID*@return
     *                     key:YearMonth（年月）,value:その月の合計勤務時間（分）
     */

    @Transactional(readOnly = true)
    public Map<YearMonth, Long> getMonthlyWorkSummary(Integer targetUserId) {

        // 1) 対象ユーザー存在チェック（論理削除も含めて確認）
        User targetUser = requireActiveTargetUser(targetUserId);

        // 2) 集計対象期間の決定
        // 仕様：当月を含めた直近12か月分
        YearMonth thisMonth = YearMonth.now(ZoneId.of("Asia/Tokyo"));
        YearMonth oldestMonth = thisMonth.minusMonths(11); // 12か月前まで

        LocalDate fromDate = oldestMonth.atDay(1); // 最古月の1日
        LocalDate toDate = thisMonth.atEndOfMonth(); // 今月の末日

        // 3) 対象期間の勤怠レコード一覧を取得
        List<AttendanceRecord> records = attendanceRecordRepository.findByUserIdBetweenDates(
                targetUser.getId(),
                fromDate,
                toDate);

        // ) 月ごとに勤務時間（分）を集計する
        // 新しい月 → 古い月 の順にしたいので reverseOrder を指定

        Map<YearMonth, Long> monthlyMinutesMap = new TreeMap<>(Comparator.reverseOrder());

        for (AttendanceRecord record : records) {

            // 4-1) 退勤していないレコード（clockOut が null）は集計対象外にする
            if (record.isUnfinished()) { // isUnfinished() がなければ clockOut == null でもOK
                continue;
            }

            // 4-2) 勤務日から YearMonth（YYYY-MM）を取り出す
            YearMonth ym = YearMonth.from(record.getWorkDate());

            // 4-3) その日の勤務時間（分）を計算
            long workingMinutes = record.calculateWorkingMinutes();
            // ↑ AttendanceRecord に「勤務時間（分）を返す」メソッドがある前提

            // 4-4) 月ごとの合計に加算
            monthlyMinutesMap.merge(ym, workingMinutes, Long::sum);
        }

        return monthlyMinutesMap;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecord> getDailyWorkRecords(Integer targetUserId) {

        // 対象ユーザー存在チェック（論理削除も含めて確認）
        User targetUser = requireActiveTargetUser(targetUserId);

        // 直近365日分の期間を計算(未退勤レコードも含めている
        LocalDate toDate = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        LocalDate fromDate = toDate.minusDays(364);

        return attendanceRecordRepository.findByUserIdBetweenDates(
                targetUser.getId(),
                fromDate,
                toDate);
    }

    @Transactional
    public void softDeleteUser(Integer targetUserId) {
        User targetUser = requireActiveTargetUser(targetUserId);

        targetUser.setDeletedAt(OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        userRepository.save(targetUser);
    }

    // 共通：有効なユーザーであることを確認して取得する。
    private User requireActiveTargetUser(Integer userId) {
        if (userId == null) {
            throw new BusinessException("error.user.notFound");
        }
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException("error.user.notFound"));
    }

    // ===== 共通：トリム系 =====

    /**
     * 前後の空白を除去して返す（null は null のまま）
     */
    private String trimOrNull(String value) {
        return (value == null) ? null : value.trim();
    }

    /**
     * 前後の空白を除去し、空欄（"" や " "）なら null を返す
     * 任意入力項目（email/phone/password）向け
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    // ===== 共通：文字数チェック =====

    /**
     * 最大文字数チェック（null はOK）
     */
    private void validateMaxLength(String value, int max, String messageKey) {
        if (value == null) {
            return;
        }
        if (value.length() > max) {
            throw new BusinessException(messageKey);
        }
    }

    /**
     * ユーザー入力の文字数チェック（DB定義＋確定ルールに合わせる）
     */
    private void validateUserInputLength(
            String username,
            String displayName,
            String email,
            String phone,
            String password) {
        // DB定義に合わせる
        validateMaxLength(username, 50, "validation.username.max50");
        validateMaxLength(displayName, 100, "validation.displayName.max100");
        validateMaxLength(email, 255, "validation.email.max255");
        validateMaxLength(phone, 20, "validation.phone.max20");

        // ★確定ルール：パスワードは72以下（入力がある場合のみチェックされる想定）
        validateMaxLength(password, 72, "validation.password.max72");
    }

}
