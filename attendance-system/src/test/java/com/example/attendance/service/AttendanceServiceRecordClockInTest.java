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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.common.BusinessException;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceRecordClockInTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private UserRepository userRepository;

    /**
     * @Spy = 実メソッドを動かしつつ、一部だけ差し替え（スタブ）できる
     *      ※「スタブ」＝特定の呼び出し結果だけをテスト用に固定すること
     */
    @Spy
    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void recordClockIn_whenNotWorking_savesNewRecord() {
        // Arrange
        Integer userId = 1;

        User user = new User();
        user.setId(userId);

        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.empty());

        // Service内の「別メソッド呼び出し」はテストでは固定してしまう（設計書の“DBアクセスはモック化”に相当）
        doReturn(user).when(attendanceService).requireActiveCurrentUser(userId);
        doReturn("会社").when(attendanceService).getCurrentWorkLocationName(userId);

        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-01-05T10:00:00+09:00");
        doReturn(fixedNow).when(attendanceService).nowTokyo();

        // Act
        attendanceService.recordClockIn(userId);

        // Assert
        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRecordRepository, times(1)).save(captor.capture());

        AttendanceRecord saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(user, saved.getUser());
        assertNull(saved.getDeletedAt());
        assertEquals(fixedNow, saved.getCreatedAt());
        assertEquals(fixedNow, saved.getUpdatedAt());

        // startWork(now, workLocationName) の結果が getter
        // で取れるなら、ここも検証できます（プロジェクトの実装に合わせて調整）
        // 例:
        // assertEquals(fixedNow, saved.getClockIn());
        // assertEquals("会社", saved.getWorkLocationName());
    }

    @Test
    void recordClockIn_whenAlreadyWorking_throwsBusinessException() {
        Integer userId = 1;

        User user = new User();
        user.setId(userId);
        doReturn(user).when(attendanceService).requireActiveCurrentUser(userId);

        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.of(new AttendanceRecord()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> attendanceService.recordClockIn(userId));

        assertEquals("error.attendance.alreadyClockIn", ex.getMessageKey());
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void recordClockIn_whenDbInsertFails_throwsException() {
        // Arrange
        Integer userId = 1;

        User user = new User();
        user.setId(userId);

        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.empty());

        doReturn(user).when(attendanceService).requireActiveCurrentUser(userId);
        doReturn("会社").when(attendanceService).getCurrentWorkLocationName(userId);
        doReturn(OffsetDateTime.parse("2026-01-05T10:00:00+09:00")).when(attendanceService).nowTokyo();

        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenThrow(new RuntimeException("DB INSERT failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> attendanceService.recordClockIn(userId));
    }

    @Test
    void recordClockIn_whenTimeProviderFails_throwsException() {
        // Arrange
        Integer userId = 1;

        User user = new User();
        user.setId(userId);

        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.empty());

        doReturn(user).when(attendanceService).requireActiveCurrentUser(userId);
        doReturn("会社").when(attendanceService).getCurrentWorkLocationName(userId);

        // ★ 設計書 No.6 相当（システム時刻の取得で例外）
        doThrow(new RuntimeException("time error")).when(attendanceService).nowTokyo();

        // Act & Assert
        assertThrows(RuntimeException.class, () -> attendanceService.recordClockIn(userId));
        verify(attendanceRecordRepository, never()).save(any());
    }
}
