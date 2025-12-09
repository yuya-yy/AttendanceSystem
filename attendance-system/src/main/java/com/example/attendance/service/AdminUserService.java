package com.example.attendance.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
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
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
            DepartmentRepository departmentRepository,
            WorkLocationRepository workLocationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.workLocationRepository = workLocationRepository;
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

        // ===== 0) 前処理：前後の空白を除去 =====
        String trimmedUsername = (username != null) ? username.trim() : null;
        String trimmedDisplayName = (displayName != null) ? displayName.trim() : null;
        String trimmedEmail = (email != null) ? email.trim() : null;
        String trimmedPhone = (phone != null) ? phone.trim() : null;

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
        if (trimmedPhone != null && !trimmedPhone.isBlank()) {
            if (!trimmedPhone.matches("^[0-9]+$")) {
                throw new BusinessException("validation.phone.numeric");
            }
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

        // ===== 4) 所属部署マスタの取得 =====
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("error.user.department.notFound"));

        // ===== 5) デフォルト勤務場所マスタの取得（null 許可） =====
        WorkLocation defaultWorkLocation = null;
        if (defaultWorkLocationId != null) {
            defaultWorkLocation = workLocationRepository.findById(defaultWorkLocationId)
                    .orElseThrow(() -> new BusinessException("error.user.workLocation.notFound"));
        }

        // ===== 6) メール・電話の「空文字 → null」正規化 =====
        String normalizedEmail = (trimmedEmail == null || trimmedEmail.isBlank()) ? null : trimmedEmail;
        String normalizedPhone = (trimmedPhone == null || trimmedPhone.isBlank()) ? null : trimmedPhone;

        // ===== 7) パスワードをハッシュ化 =====
        String passwordHash = passwordEncoder.encode(rawPassword);

        // ===== 8) User エンティティを作成 =====
        OffsetDateTime now = OffsetDateTime.now();

        User user = new User();
        user.setUsername(trimmedUsername);
        user.setDisplayName(trimmedDisplayName);
        user.setPasswordHash(passwordHash);
        user.setEmail(normalizedEmail); // ★ 空欄なら DB では NULL
        user.setPhone(normalizedPhone); // ★ 空欄なら DB では NULL
        user.setRole(role);
        user.setDepartment(department);
        user.setDefaultWorkLocation(defaultWorkLocation);
        user.setDeletedAt(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // ===== 9) 保存 =====
        userRepository.save(user);
    }
}
