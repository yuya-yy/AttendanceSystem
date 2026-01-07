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
class AdminUserServiceRegisterUserTest {

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

    @Test
    void registerUser_whenAllValid_registersGeneralUser_andSavesNormalizedValues() {
        // Arrange
        String username = "user001";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        String email = "user001@mail.com";
        String phone = "090-1234-5678"; // ハイフンあり入力
        Integer role = 2;
        Integer departmentId = 10;
        Integer defaultWorkLocationId = null;

        // 重複なし
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByPhone("09012345678")).thenReturn(Optional.empty());

        // 部署は存在
        Department dept = new Department();
        dept.setId(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        // パスワードハッシュ
        when(passwordEncoder.encode(rawPassword)).thenReturn("HASHED");

        // Act
        adminUserService.registerUser(
                username, displayName, rawPassword, email, phone,
                role, departmentId, defaultWorkLocationId);

        // Assert：saveされたUserの中身を検査する
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals(username, saved.getUsername());
        assertEquals(displayName, saved.getDisplayName());
        assertEquals("HASHED", saved.getPasswordHash());
        assertEquals(email, saved.getEmail());
        assertEquals("09012345678", saved.getPhone()); // ★数字だけで保存される
        assertEquals(role, saved.getRole());
        assertEquals(dept, saved.getDepartment());
        assertNull(saved.getDefaultWorkLocation());
        assertNull(saved.getDeletedAt());

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt()); // 同じnowを入れている実装なので一致するはず
    }

    @Test
    void registerUser_whenEmailAndPhoneBlank_savesNull_andDoesNotCheckDuplicate() {
        // Arrange
        String username = "user002";
        String displayName = "メールなし";
        String rawPassword = "Abc123!";
        String email = "   "; // 空欄扱い
        String phone = ""; // 空欄扱い
        Integer role = 2;
        Integer departmentId = 10;
        Integer defaultWorkLocationId = null;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Department dept = new Department();
        dept.setId(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        when(passwordEncoder.encode(rawPassword)).thenReturn("HASHED");

        // Act
        adminUserService.registerUser(
                username, displayName, rawPassword, email, phone,
                role, departmentId, defaultWorkLocationId);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertNull(saved.getEmail()); // ★空欄はnull
        assertNull(saved.getPhone()); // ★空欄はnull

        // ★空欄なので重複チェック自体をしない想定
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).findByPhone(any());
    }

    @Test
    void registerUser_whenUsernameBlank_throwsRequired() {
        // Arrange
        String username = "   ";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("error.auth.username.required", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenUsernameDuplicate_throwsDuplicate() {
        // Arrange
        String username = "user001";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        // ★ここが本体：ユーザー名が既に存在
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new User()));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("validation.username.duplicate", ex.getMessageKey());

        // ★重複で止まるので保存しない
        verify(userRepository, never()).save(any());
        // ★部署取得などにも進まない（進むとstubが必要になりやすい）
        verify(departmentRepository, never()).findById(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerUser_whenUsernameHasInvalidChars_throwsFormatError() {
        // Arrange
        String username = "user-001"; // "-" があるのでNG
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("validation.username.alnum", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenPasswordBlank_throwsRequired() {
        // Arrange
        String username = "user003";
        String displayName = "山田太郎";
        String rawPassword = "   ";
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("error.auth.password.required", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenPasswordHasInvalidChars_throwsFormatError() {
        // Arrange
        String username = "user010";
        String displayName = "山田太郎";
        String rawPassword = "abc123あ"; // ★全角が混ざってNG
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("validation.password.alnumSymbol", ex.getMessageKey());

        // ★形式チェックで止まるので、DB/Encoderに一切触れない想定
        verifyNoInteractions(userRepository);
        verifyNoInteractions(departmentRepository);
        verifyNoInteractions(workLocationRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerUser_whenEmailInvalid_throwsFormatError() {
        // Arrange
        String username = "user003";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        String email = "user.mail.com"; // ← 不正（@がない等）
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username,
                displayName,
                rawPassword,
                email,
                null, // phone
                role,
                departmentId,
                null // defaultWorkLocationId
        ));

        assertEquals("validation.email.format", ex.getMessageKey());

        // 形式チェックで止まるなら、DB/Encoderには触れない
        verifyNoInteractions(userRepository);
        verifyNoInteractions(departmentRepository);
        verifyNoInteractions(workLocationRepository);
        verifyNoInteractions(attendanceRecordRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerUser_whenEmailDuplicate_throwsDuplicate() {
        // Arrange
        String username = "user005";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        String email = "dup@mail.com";
        Integer role = 2;
        Integer departmentId = 10;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User())); // 重複あり

        Department dept = new Department();
        dept.setId(departmentId);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                email, null, role, departmentId, null));

        assertEquals("error.user.email.duplicate", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenPhoneHasNonDigits_throwsFormatError() {
        // Arrange
        String username = "user006";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        String phone = "090-ABCD-5678";
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, phone, role, departmentId, null));

        assertEquals("validation.phone.numeric", ex.getMessageKey());

        // 形式チェックで止まるので、DB系には一切触れないことを確認（任意だけどオススメ）
        verifyNoInteractions(userRepository);
        verifyNoInteractions(departmentRepository);
        verifyNoInteractions(workLocationRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerUser_whenDepartmentNotFound_throwsNotFound() {
        // Arrange
        String username = "user007";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 9999;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("error.user.department.notFound", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenDefaultWorkLocationNotFound_throwsNotFound() {
        // Arrange
        String username = "user008";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;
        Integer defaultWorkLocationId = 999;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Department dept = new Department();
        dept.setId(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        when(workLocationRepository.findById(defaultWorkLocationId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, defaultWorkLocationId));

        assertEquals("error.user.workLocation.notFound", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenSaveThrows_propagatesRuntimeException() {
        // Arrange
        String username = "user009";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Department dept = new Department();
        dept.setId(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        when(passwordEncoder.encode(rawPassword)).thenReturn("HASHED");
        doThrow(new RuntimeException("DB down")).when(userRepository).save(any(User.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));
    }

    @Test
    void registerUser_whenUsernameAndDisplayNameHaveSpaces_trimsAndSaves() {
        // Arrange
        String username = "  user010  ";
        String displayName = "  山田太郎  ";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        // ★ trimOrNull が効いて、Repository検索はトリム後で行われる想定
        when(userRepository.findByUsername("user010")).thenReturn(Optional.empty());

        Department dept = new Department();
        dept.setId(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        when(passwordEncoder.encode(rawPassword)).thenReturn("HASHED");

        // Act
        adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("user010", saved.getUsername()); // ★トリムされている
        assertEquals("山田太郎", saved.getDisplayName()); // ★トリムされている

        // 検索もトリム後で呼ばれていること
        verify(userRepository).findByUsername("user010");
    }

    @Test
    void registerUser_whenUsernameOver50_throwsMax50() {
        // Arrange
        String username = "A".repeat(51); // 51文字
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("validation.username.max50", ex.getMessageKey());

        // ★文字数チェックで止まるので、DB/Encoderには触れない
        verifyNoInteractions(userRepository, departmentRepository, workLocationRepository, passwordEncoder);
    }

    @Test
    void registerUser_whenDisplayNameOver100_throwsMax100() {
        // Arrange
        String username = "user011";
        String displayName = "あ".repeat(101); // 101文字
        String rawPassword = "Abc123!";
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("validation.displayName.max100", ex.getMessageKey());

        verifyNoInteractions(userRepository, departmentRepository, workLocationRepository, passwordEncoder);
    }

    @Test
    void registerUser_whenEmailOver255_throwsMax255() {
        // Arrange
        String username = "user012";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        String email = "a".repeat(256); // 256文字
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                email, null, role, departmentId, null));

        assertEquals("validation.email.max255", ex.getMessageKey());

        verifyNoInteractions(userRepository, departmentRepository, workLocationRepository, passwordEncoder);
    }

    @Test
    void registerUser_whenPhoneOver20_throwsMax20() {
        // Arrange
        String username = "user013";
        String displayName = "山田太郎";
        String rawPassword = "Abc123!";
        String phone = "1".repeat(21); // 21文字（DBは20まで）
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, phone, role, departmentId, null));

        assertEquals("validation.phone.max20", ex.getMessageKey());

        verifyNoInteractions(userRepository, departmentRepository, workLocationRepository, passwordEncoder);
    }

    @Test
    void registerUser_whenPasswordOver72_throwsMax72() {
        // Arrange
        String username = "user014";
        String displayName = "山田太郎";
        String rawPassword = "A".repeat(73); // 73文字（確定ルールは72まで）
        Integer role = 2;
        Integer departmentId = 10;

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.registerUser(
                username, displayName, rawPassword,
                null, null, role, departmentId, null));

        assertEquals("validation.password.max72", ex.getMessageKey());

        verifyNoInteractions(userRepository, departmentRepository, workLocationRepository, passwordEncoder);
    }

}
