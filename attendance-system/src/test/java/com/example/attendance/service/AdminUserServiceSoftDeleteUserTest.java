package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceSoftDeleteUserTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User createActiveUser(Integer id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user001");
        u.setDisplayName("山田太郎");
        u.setEmail("user001@mail.com");
        u.setPhone("09012345678");
        u.setRole(2);
        u.setDeletedAt(null); // 有効
        return u;
    }

    @Test
    void softDeleteUser_whenUserExistsAndActive_setsDeletedAt_andSaves() {
        // No1: 削除対象ユーザーが存在し、有効な場合、論理削除することができる
        Integer targetUserId = 1;
        User user = createActiveUser(targetUserId);

        when(userRepository.findByIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(user));

        adminUserService.softDeleteUser(targetUserId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals(targetUserId, saved.getId());
        assertNotNull(saved.getDeletedAt()); // deleted_at が入る
        assertEquals(ZoneOffset.ofHours(9), saved.getDeletedAt().getOffset()); // Asia/Tokyo想定(+09)

        // 他の項目は基本変えない（今回の実装上）
        assertEquals("user001", saved.getUsername());
        assertEquals("山田太郎", saved.getDisplayName());
        assertEquals("user001@mail.com", saved.getEmail());
        assertEquals("09012345678", saved.getPhone());
        assertEquals(2, saved.getRole());
    }

    @Test
    void softDeleteUser_whenUserHasAttendanceRecords_stillSucceeds_andOnlyUpdatesDeletedAt() {
        // No2: 勤怠記録が存在しても、ユーザーを論理削除することができる
        // ※このService実装は attendance_records を触らないので、
        // 「勤怠が残ること」は結合テスト(DBあり)で確認すべきです。
        // ユニットテストでは「他のRepository等に触れない」ことを確認するのが現実的。

        Integer targetUserId = 2;
        User user = createActiveUser(targetUserId);

        when(userRepository.findByIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(user));

        adminUserService.softDeleteUser(targetUserId);

        verify(userRepository).save(any(User.class));
        assertNotNull(user.getDeletedAt());
    }

    @Test
    void softDeleteUser_whenUserNotFound_throwsNotFound() {
        // No3: 削除対象ユーザーが存在しない場合
        Integer targetUserId = 9999;

        when(userRepository.findByIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.softDeleteUser(targetUserId));

        assertEquals("error.user.notFound", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void softDeleteUser_whenUserIdIsNull_throwsNotFound_withoutRepositoryCall() {
        // （設計書に明記がなくても、実装にあるのでテストしておくのはおすすめ）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.softDeleteUser(null));

        assertEquals("error.user.notFound", ex.getMessageKey());
        verifyNoInteractions(userRepository);
    }

    @Test
    void softDeleteUser_whenSaveThrows_propagatesRuntimeException() {
        // No5: save時に例外が発生した場合（Serviceは握りつぶさず投げ返す）
        Integer targetUserId = 1;
        User user = createActiveUser(targetUserId);

        when(userRepository.findByIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("DB down")).when(userRepository).save(any(User.class));

        assertThrows(RuntimeException.class, () -> adminUserService.softDeleteUser(targetUserId));
    }

    /*
     * No4（すでに削除済みユーザーを再削除）について：
     * 現在の実装は findByIdAndDeletedAtIsNull なので、削除済みは「存在しない」と同じ扱いになります。
     * つまり No4 を「ERROR_USER_ALREADY_DELETED」で分けて返すことは、この実装だけだとできません。
     *
     * もし設計書どおりに「削除済み」を区別したいなら、
     * findById() で取って deletedAt != null をチェックする実装に変える必要があります。
     */
}
