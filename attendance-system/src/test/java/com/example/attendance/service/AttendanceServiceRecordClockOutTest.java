package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceRecordClockOutTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void recordClockOut_whenHasUnfinished_finishesAndSaves() {
        // Arrange
        Integer userId = 1;

        // 先頭の存在チェックはテストでは固定でOK
        doReturn(new User()).when(attendanceService).requireActiveCurrentUser(userId);

        AttendanceRecord record = spy(new AttendanceRecord());
        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.of(record));

        doNothing().when(record).finishWork(any(OffsetDateTime.class));

        // Act
        attendanceService.recordClockOut(userId);

        // Assert
        verify(attendanceRecordRepository).findLatestUnfinished(userId);
        verify(record).finishWork(any(OffsetDateTime.class));
        verify(record).setUpdatedAt(any(OffsetDateTime.class));
        verify(attendanceRecordRepository).save(record);
    }

    // （参考）未退勤が無い → BusinessException（alreadyClockOut）
    @Test
    void recordClockOut_whenNoUnfinished_throwsBusinessException() {
        // Arrange
        Integer userId = 1;

        doReturn(new User()).when(attendanceService).requireActiveCurrentUser(userId);

        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> attendanceService.recordClockOut(userId));

        assertEquals("error.attendance.alreadyClockOut", ex.getMessageKey());
        verify(attendanceRecordRepository, never()).save(any());
    }

    // No.5（異常）DB取得時（findLatestUnfinished）に例外 → 例外がそのまま出る / saveされない
    @Test
    void recordClockOut_whenFindThrows_throwsException_andNeverSaves() {
        // Arrange
        Integer userId = 1;

        doReturn(new User()).when(attendanceService).requireActiveCurrentUser(userId);

        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenThrow(new RuntimeException("DB read error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> attendanceService.recordClockOut(userId));
        verify(attendanceRecordRepository, never()).save(any());
    }

    // No.6（異常）DB更新（save）時に例外 → 例外がそのまま出る
    @Test
    void recordClockOut_whenSaveThrows_throwsException() {
        // Arrange
        Integer userId = 1;

        doReturn(new User()).when(attendanceService).requireActiveCurrentUser(userId);

        AttendanceRecord record = spy(new AttendanceRecord());
        when(attendanceRecordRepository.findLatestUnfinished(userId))
                .thenReturn(Optional.of(record));

        doNothing().when(record).finishWork(any(OffsetDateTime.class));

        when(attendanceRecordRepository.save(record))
                .thenThrow(new RuntimeException("DB update error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> attendanceService.recordClockOut(userId));
        verify(record).finishWork(any(OffsetDateTime.class));
        verify(record).setUpdatedAt(any(OffsetDateTime.class));
    }
}
