package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceScreenSupportTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkLocationRepository workLocationRepository; // （このテスト範囲では未使用でも@InjectMocks用に置く）

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void findLatestUnfinished_whenExists_returnsRecord() {
        AttendanceRecord record = new AttendanceRecord();
        when(attendanceRecordRepository.findLatestUnfinished(1)).thenReturn(Optional.of(record));

        AttendanceRecord result = attendanceService.findLatestUnfinished(1);

        assertSame(record, result);
    }

    @Test
    void findLatestUnfinished_whenEmpty_returnsNull() {
        when(attendanceRecordRepository.findLatestUnfinished(1)).thenReturn(Optional.empty());

        AttendanceRecord result = attendanceService.findLatestUnfinished(1);

        assertNull(result);
    }

    @Test
    void getRecentRecords_callsRepositoryWithLast30DaysRange() {
        when(attendanceRecordRepository.findByUserIdBetweenDates(anyInt(), any(), any()))
                .thenReturn(List.of());

        attendanceService.getRecentRecords(1);

        // 引数を捕まえて「30日分（当日含む）」になっているか確認
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

        verify(attendanceRecordRepository).findByUserIdBetweenDates(eq(1), fromCaptor.capture(), toCaptor.capture());

        LocalDate from = fromCaptor.getValue();
        LocalDate to = toCaptor.getValue();

        // to - from が 29日なら「当日含めて30日分」
        long days = ChronoUnit.DAYS.between(from, to);
        assertEquals(29, days);
    }

    @Test
    void getCurrentWorkLocationName_whenDefaultNull_returnsMisetting() {
        User user = new User();
        user.setId(1);
        user.setDeletedAt(null);
        user.setDefaultWorkLocation(null);

        when(userRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(user));

        String result = attendanceService.getCurrentWorkLocationName(1);

        assertEquals("未設定", result);
    }

    @Test
    void getCurrentWorkLocationName_whenDefaultExists_returnsLocationName() {
        WorkLocation wl = new WorkLocation();
        wl.setLocationName("会社");

        User user = new User();
        user.setId(1);
        user.setDeletedAt(null);
        user.setDefaultWorkLocation(wl);

        when(userRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(user));

        String result = attendanceService.getCurrentWorkLocationName(1);

        assertEquals("会社", result);
    }
}