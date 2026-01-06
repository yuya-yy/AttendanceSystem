package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceUpdateWorkLocationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkLocationRepository workLocationRepository;

    @InjectMocks
    private UserSettingService userSettingService;

    /**
     * ★ テスト用：有効な「ログイン中ユーザー」をモックで返す設定
     * requireActiveCurrentUser() が内部でどのメソッドを呼んでも通るように、両方 stub しておく
     */
    private User stubActiveCurrentUser(Integer userId) {
        User user = new User();
        user.setId(userId);
        user.setDeletedAt(null); // active

        // ★ここが超重要：Serviceが呼ぶRepositoryメソッドに合わせて stub する
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        return user;
    }

    @Test
    void updateWorkLocation_whenOk_setsDefaultAndSaves() {
        // Arrange
        Integer userId = 1;
        Integer workLocationId = 10;

        User user = stubActiveCurrentUser(userId);

        WorkLocation loc = new WorkLocation();
        loc.setId(workLocationId);
        loc.setDeletedAt(null); // active

        when(workLocationRepository.findById(workLocationId)).thenReturn(Optional.of(loc));

        // Act
        userSettingService.updateWorkLocation(userId, workLocationId);

        // Assert
        assertEquals(loc, user.getDefaultWorkLocation());
        assertNotNull(user.getUpdatedAt());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(user, captor.getValue());
    }

    @Test
    void updateWorkLocation_whenAlreadySet_updatesToDifferentLocation_andSaves() {
        // Arrange
        Integer userId = 1;

        // 既に登録済みの勤務場所（A）
        WorkLocation current = new WorkLocation();
        current.setId(1);
        current.setDeletedAt(null);

        User user = stubActiveCurrentUser(userId);
        user.setDefaultWorkLocation(current);

        // 更新先（B）
        Integer newWorkLocationId = 10;
        WorkLocation newLoc = new WorkLocation();
        newLoc.setId(newWorkLocationId);
        newLoc.setDeletedAt(null);

        when(workLocationRepository.findById(newWorkLocationId)).thenReturn(Optional.of(newLoc));

        // Act
        userSettingService.updateWorkLocation(userId, newWorkLocationId);

        // Assert
        assertEquals(newLoc, user.getDefaultWorkLocation()); // Bに変わる
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
    }

    @Test
    void updateWorkLocation_whenSameLocation_stillSucceeds_andSaves() {
        // Arrange
        Integer userId = 1;

        Integer workLocationId = 10;
        WorkLocation loc = new WorkLocation();
        loc.setId(workLocationId);
        loc.setDeletedAt(null);

        User user = stubActiveCurrentUser(userId);
        user.setDefaultWorkLocation(loc); // すでに同じ場所

        when(workLocationRepository.findById(workLocationId)).thenReturn(Optional.of(loc));

        // Act (例外が出ないことを確認)
        assertDoesNotThrow(() -> userSettingService.updateWorkLocation(userId, workLocationId));

        // Assert
        assertEquals(loc, user.getDefaultWorkLocation()); // そのまま
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
    }

    @Test
    void updateWorkLocation_whenNull_throwsRequired() {
        // Arrange
        Integer userId = 1;
        stubActiveCurrentUser(userId);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateWorkLocation(userId, null));

        assertEquals("error.workLocation.required", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateWorkLocation_whenWorkLocationNotFound_throwsNotFound() {
        // Arrange
        Integer userId = 1;
        Integer workLocationId = 99;

        stubActiveCurrentUser(userId);
        when(workLocationRepository.findById(workLocationId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateWorkLocation(userId, workLocationId));

        assertEquals("error.workLocation.notFound", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateWorkLocation_whenWorkLocationInactive_throwsNotFound() {
        // Arrange
        Integer userId = 1;
        Integer workLocationId = 60;

        stubActiveCurrentUser(userId);

        WorkLocation inactive = new WorkLocation();
        inactive.setId(workLocationId);
        inactive.setDeletedAt(OffsetDateTime.now()); // inactive

        when(workLocationRepository.findById(workLocationId)).thenReturn(Optional.of(inactive));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSettingService.updateWorkLocation(userId, workLocationId));

        // 現実装は inactive も notFound 扱い
        assertEquals("error.workLocation.notFound", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateWorkLocation_whenSaveThrows_throwsException() {
        // Arrange
        Integer userId = 1;
        Integer workLocationId = 10;

        User user = stubActiveCurrentUser(userId);

        WorkLocation loc = new WorkLocation();
        loc.setId(workLocationId);
        loc.setDeletedAt(null);

        when(workLocationRepository.findById(workLocationId)).thenReturn(Optional.of(loc));

        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("DB save error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> userSettingService.updateWorkLocation(userId, workLocationId));

        // save まで行って例外になっていること
        verify(userRepository).save(user);
    }
}
