package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceUpdateUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private WorkLocationRepository workLocationRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    // ===== テスト用ヘルパー =====

    /** 対象ユーザー（有効）を findById で返す前提の stub。 */
    private User stubActiveTargetUser(Integer userId) {
        User user = new User();
        user.setId(userId);
        user.setDeletedAt(null); // 有効
        user.setUsername("user001");
        user.setDisplayName("山田太郎");
        user.setEmail("user001@mail.com");
        user.setPhone("09012345678");
        user.setRole(2);
        user.setPasswordHash("OLD_HASH");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        return user;
    }

    private Department stubDepartment(Integer departmentId) {
        Department dept = new Department();
        dept.setId(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));
        return dept;
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    // ===== 正常系（Excel No1〜No6 相当） =====

    @Test
    void updateUser_whenChangeDisplayNameAndPhone_updatesAndSaves() {
        // No1: 氏名と電話番号だけ変更できる
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        Department dept = stubDepartment(10);

        // 重複チェック（自分は除外される想定）
        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user001@mail.com")).thenReturn(Optional.of(user));
        when(userRepository.findByPhone("08011112222")).thenReturn(Optional.empty());

        // Act
        adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎2",
                "user001@mail.com",
                "08011112222", // ハイフンなし
                10,
                2,
                "" // パスワード空欄＝変更なし
        );

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("user001", saved.getUsername());
        assertEquals("山田太郎2", saved.getDisplayName());
        assertEquals("user001@mail.com", saved.getEmail());
        assertEquals("08011112222", saved.getPhone());
        assertEquals(2, saved.getRole());
        assertEquals(dept, saved.getDepartment());
        assertEquals("OLD_HASH", saved.getPasswordHash()); // 変更なし
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_whenChangeUsernameToNewUnique_updatesAndSaves() {
        // No2: ユーザー名を未使用の値に変更できる
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        Department dept = stubDepartment(10);

        when(userRepository.findByUsername("user001new")).thenReturn(Optional.empty()); // 未使用
        when(userRepository.findByEmail("user001@mail.com")).thenReturn(Optional.of(user)); // 自分
        when(userRepository.findByPhone("09012345678")).thenReturn(Optional.of(user)); // 自分（同じ電話）

        // Act
        adminUserService.updateUser(
                targetUserId,
                "user001new",
                "山田太郎",
                "user001@mail.com",
                "090-1234-5678", // ハイフンあり → 数字のみ保存
                10,
                2,
                null);

        // Assert
        verify(userRepository).save(user);
        assertEquals("user001new", user.getUsername());
        assertEquals("09012345678", user.getPhone());
        assertEquals(dept, user.getDepartment());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_whenChangeEmailToNewUnique_updatesAndSaves() {
        // No3: メールを正しい形式の別アドレスに変更できる
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        Department dept = stubDepartment(10);

        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user)); // 自分
        when(userRepository.findByEmail("new001@mail.com")).thenReturn(Optional.empty()); // 未使用

        // phone=null → findByPhone は呼ばれない

        // Act
        adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                "new001@mail.com",
                null,
                10,
                2,
                null);

        // Assert
        verify(userRepository).save(user);
        assertEquals("new001@mail.com", user.getEmail());
        assertEquals(dept, user.getDepartment());
    }

    @Test
    void updateUser_whenPasswordProvided_updatesPasswordHash_andSaves() {
        // No4: パスワード欄に入力があれば password_hash を更新できる
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        Department dept = stubDepartment(10);

        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user001@mail.com")).thenReturn(Optional.of(user));

        when(passwordEncoder.encode("NewPass#123")).thenReturn("NEW_HASH");

        // Act
        adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                "user001@mail.com",
                null,
                10,
                2,
                "NewPass#123");

        // Assert
        verify(userRepository).save(user);
        assertEquals("NEW_HASH", user.getPasswordHash());
        assertEquals(dept, user.getDepartment());
        verify(passwordEncoder).encode("NewPass#123");
    }

    @Test
    void updateUser_whenNoChangeStillSucceeds_andSaves() {
        // No5: 何も変更しなくても成功扱い（エラーにしない）
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        Department dept = stubDepartment(10);

        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user001@mail.com")).thenReturn(Optional.of(user));
        when(userRepository.findByPhone("09012345678")).thenReturn(Optional.of(user)); // 自分

        // Act
        assertDoesNotThrow(() -> adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                "user001@mail.com",
                "090-1234-5678",
                10,
                2,
                "" // 変更なし
        ));

        // Assert
        verify(userRepository).save(user);
        assertEquals(dept, user.getDepartment());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_whenRoleChangedWithinRange_updatesAndSaves() {
        // No6: role が 1/2 の範囲なら更新できる（※現実装は範囲チェック自体は無い）
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        Department dept = stubDepartment(10);

        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user001@mail.com")).thenReturn(Optional.of(user));

        // Act
        adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                "user001@mail.com",
                null,
                10,
                1, // 管理者へ
                null);

        // Assert
        verify(userRepository).save(user);
        assertEquals(1, user.getRole());
        assertEquals(dept, user.getDepartment());
    }

    // ===== 異常系（Excel No7〜No12, No17 相当） =====

    @Test
    void updateUser_whenUsernameHasInvalidChars_throwsFormatError() {
        // No7: ユーザー名に半角英数字以外が含まれる
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1,
                "user-001",
                "山田太郎",
                null,
                null,
                10,
                2,
                null));
        assertEquals("validation.username.alnum", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenUsernameDuplicateOtherUser_throwsDuplicate() {
        // No8: 他ユーザーのユーザー名に変更 → 重複エラー（自分は除外）
        Integer targetUserId = 1;

        stubActiveTargetUser(targetUserId);

        User other = new User();
        other.setId(2);
        when(userRepository.findByUsername("user002")).thenReturn(Optional.of(other));

        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                targetUserId,
                "user002",
                "山田太郎",
                null,
                null,
                10,
                2,
                null));

        assertEquals("validation.username.duplicate", ex.getMessageKey());
        verify(userRepository, never()).save(any());
        verify(departmentRepository, never()).findById(any());
    }

    @Test
    void updateUser_whenEmailInvalid_throwsFormatError() {
        // No9: メール形式不正
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1,
                "user001",
                "山田太郎",
                "user001.mail.com",
                null,
                10,
                2,
                null));
        assertEquals("validation.email.format", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenEmailDuplicateOtherUser_throwsDuplicate() {
        // No10: 他ユーザーのメールに変更 → 重複エラー（自分は除外）
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);

        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));

        User other = new User();
        other.setId(2);
        when(userRepository.findByEmail("user002@mail.com")).thenReturn(Optional.of(other));

        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                "user002@mail.com",
                null,
                10,
                2,
                null));

        assertEquals("error.user.email.duplicate", ex.getMessageKey());
        verify(userRepository, never()).save(any());
        verify(departmentRepository, never()).findById(any());
    }

    @Test
    void updateUser_whenPhoneHasNonDigits_throwsFormatError() {
        // No11: 電話番号に数字以外が混ざる
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1,
                "user001",
                "山田太郎",
                null,
                "090-ABCD-5678",
                10,
                2,
                null));
        assertEquals("validation.phone.numeric", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenDepartmentNotFound_throwsNotFound() {
        // No12: departmentId が存在しない
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));

        when(departmentRepository.findById(9999)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                null,
                null,
                9999,
                2,
                null));

        assertEquals("error.user.department.notFound", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_whenSaveThrows_propagatesRuntimeException() {
        // No17: save で例外 → そのまま投げ返される（Controllerで system error にする想定）
        Integer targetUserId = 1;

        User user = stubActiveTargetUser(targetUserId);
        stubDepartment(10);

        when(userRepository.findByUsername("user001")).thenReturn(Optional.of(user));

        doThrow(new RuntimeException("DB down")).when(userRepository).save(any(User.class));

        assertThrows(RuntimeException.class, () -> adminUserService.updateUser(
                targetUserId,
                "user001",
                "山田太郎",
                null,
                null,
                10,
                2,
                null));
    }

    // ===== 追加: パスワード形式不正 / ユーザー名重複（あなたが追加した2件） =====

    @Test
    void updateUser_whenPasswordHasInvalidChars_throwsFormatError() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1,
                "user010",
                "山田太郎",
                null,
                null,
                10,
                2,
                "abc123あ" // 全角混入
        ));

        assertEquals("validation.password.alnumSymbol", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    // ===== 追加: 文字数チェック（あなたが貼った validateUserInputLength の分） =====

    @Test
    void updateUser_whenUsernameTooLong_throwsMax50() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1, repeat('a', 51), "山田太郎", null, null, 10, 2, null));
        assertEquals("validation.username.max50", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenDisplayNameTooLong_throwsMax100() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1, "user001", repeat('あ', 101), null, null, 10, 2, null));
        assertEquals("validation.displayName.max100", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenEmailTooLong_throwsMax255() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1, "user001", "山田太郎", repeat('a', 256), null, 10, 2, null));
        assertEquals("validation.email.max255", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenPhoneTooLong_throwsMax20() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1, "user001", "山田太郎", null, repeat('1', 21), 10, 2, null));
        assertEquals("validation.phone.max20", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void updateUser_whenPasswordTooLong_throwsMax72() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.updateUser(
                1, "user001", "山田太郎", null, null, 10, 2, repeat('a', 73)));
        assertEquals("validation.password.max72", ex.getMessageKey());
        verifyNoInteractions(userRepository, departmentRepository, passwordEncoder);
    }
}
