package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.entity.User;
import com.example.attendance.common.BusinessException;
import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceContactTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingService userSettingService;

    // テスト用：Userを作る（プロジェクトのUserにsetIdが無い場合は、ここを調整してください）
    private User createUser(Integer id, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPhone(phone);
        return user;
    }

    @Test
    @DisplayName("No.1: 初回登録（email/phoneともに正常）→ saveされる")
    void updateContactInfo_firstRegister_success() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByEmail("testuser1234@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("08012345678")).thenReturn(Optional.empty());

        userSettingService.updateContactInfo(userId, "testuser1234@mail.com", "08012345678");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("testuser1234@mail.com", saved.getEmail());
        assertEquals("08012345678", saved.getPhone());
        assertNotNull(saved.getUpdatedAt()); // 更新日時が入っていること
    }

    @Test
    @DisplayName("No.2: 両方更新（email/phoneともに正常）→ saveされる（phoneはハイフン除去）")
    void updateContactInfo_updateBoth_success() {
        Integer userId = 1;

        User loginUser = createUser(userId, "old@mail.com", "09000000000");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByEmail("admin1111@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("09011112222")).thenReturn(Optional.empty());

        userSettingService.updateContactInfo(userId, "admin1111@mail.com", "090-1111-2222");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("admin1111@mail.com", saved.getEmail());
        assertEquals("09011112222", saved.getPhone()); // ハイフンが消える
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("No.3相当: emailだけ指定・phone未指定(空) → 現実装だと phone は null に更新される")
    void updateContactInfo_emailOnly_currentImpl_clearsPhone() {
        Integer userId = 1;

        User loginUser = createUser(userId, "old@mail.com", "07011112222");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByEmail("change-2222@mail.com")).thenReturn(Optional.empty());

        // phoneは空文字（HTMLの未入力は通常これ）
        userSettingService.updateContactInfo(userId, "change-2222@mail.com", "");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("change-2222@mail.com", saved.getEmail());
        assertNull(saved.getPhone()); // ★現実装だと「未指定＝クリア」になる
    }

    @Test
    @DisplayName("No.4相当: phoneだけ指定・email未指定(空) → 現実装だと email は null に更新される")
    void updateContactInfo_phoneOnly_currentImpl_clearsEmail() {
        Integer userId = 1;

        User loginUser = createUser(userId, "old@mail.com", "07011112222");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByPhone("07022223333")).thenReturn(Optional.empty());

        userSettingService.updateContactInfo(userId, "", "070-2222-3333");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertNull(saved.getEmail()); // ★現実装だと「未指定＝クリア」になる
        assertEquals("07022223333", saved.getPhone());
    }

    @Test
    @DisplayName("No.5: メール形式が不正 → BusinessException(validation.email.format)")
    void updateContactInfo_invalidEmail_throwsBusinessException() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, "testuser-mail", "09012345678"));

        assertEquals("validation.email.format", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("No.6: 電話番号が数字以外を含む → BusinessException(validation.phone.numeric)")
    void updateContactInfo_invalidPhone_throwsBusinessException() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        // when(userRepository.findByEmail("testuser1234@mail.com")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, "testuser1234@mail.com", "aaa12345678"));

        assertEquals("validation.phone.numeric", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("No.7相当: ユーザーが見つからない → BusinessException(error.auth.userDeleted) ※現実装仕様")
    void updateContactInfo_userNotFound_throwsBusinessException() {
        Integer userId = 9999;

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, "user9999@mail.com", "09099999999"));

        // requireActiveCurrentUser() がこのキー固定なので、現実装はこれになります
        assertEquals("error.auth.userDeleted", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("No.8相当: （deleted含む想定はRepository側で弾かれる）→ 現実装だと error.auth.userDeleted 扱い")
    void updateContactInfo_userDeletedLike_throwsBusinessException() {
        Integer userId = 2002;

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, "user2002@mail.com", "09020020000"));

        assertEquals("error.auth.userDeleted", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("No.9: save時に例外 → 例外がそのまま投げられる（Serviceはcatchしない）")
    void updateContactInfo_saveThrowsException_propagates() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByEmail("testuser1234@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("08012345678")).thenReturn(Optional.empty());

        doThrow(new RuntimeException("DB error")).when(userRepository).save(any(User.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userSettingService.updateContactInfo(userId, "testuser1234@mail.com", "08012345678"));

        assertEquals("DB error", ex.getMessage());
    }

    @Test
    @DisplayName("getContactInfo: 有効ユーザーならUserを返す")
    void getContactInfo_returnsUser() {
        Integer userId = 1;

        User loginUser = createUser(userId, "a@mail.com", "09012345678");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));

        User result = userSettingService.getContactInfo(userId);

        assertSame(loginUser, result);
    }

    @Test
    @DisplayName("追加: email重複（自分以外）→ BusinessException(error.user.email.duplicate)")
    void updateContactInfo_emailDuplicate_throwsBusinessException() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        User otherUser = createUser(2, "dup@mail.com", null);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByEmail("dup@mail.com")).thenReturn(Optional.of(otherUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, "dup@mail.com", null));

        assertEquals("error.user.email.duplicate", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("追加: phone重複（自分以外）→ BusinessException(error.user.phone.duplicate)")
    void updateContactInfo_phoneDuplicate_throwsBusinessException() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        User otherUser = createUser(2, null, "09011112222");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));
        when(userRepository.findByPhone("09011112222")).thenReturn(Optional.of(otherUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, null, "090-1111-2222"));

        assertEquals("error.user.phone.duplicate", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("追加: email最大255文字超 → BusinessException(validation.email.max255)")
    void updateContactInfo_emailTooLong_throwsBusinessException() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));

        String tooLong = "a".repeat(256) + "@mail.com";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, tooLong, null));

        assertEquals("validation.email.max255", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("追加: phone最大20桁超 → BusinessException(validation.phone.max20)")
    void updateContactInfo_phoneTooLong_throwsBusinessException() {
        Integer userId = 1;

        User loginUser = createUser(userId, null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(loginUser));

        String tooLongPhone = "1".repeat(21);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateContactInfo(userId, null, tooLongPhone));

        assertEquals("validation.phone.max20", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }
}
