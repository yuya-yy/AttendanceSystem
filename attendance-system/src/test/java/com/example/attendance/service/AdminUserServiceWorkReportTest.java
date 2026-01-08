package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceWorkReportTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    // ===== helper =====
    private User stubActiveTargetUser(Integer userId) {
        User u = new User();
        u.setId(userId);
        u.setDeletedAt(null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(u));
        return u;
    }

    // ===== getMonthlyWorkSummary =====

    @Test
    void getMonthlyWorkSummary_whenRecordsExist_aggregatesByYearMonth_andSkipsUnfinished_andSortsDesc() {
        // Arrange
        Integer targetUserId = 1;
        User target = stubActiveTargetUser(targetUserId);

        // 月を固定して作る（今月/先月）
        YearMonth thisMonth = YearMonth.now(ZoneId.of("Asia/Tokyo"));
        YearMonth lastMonth = thisMonth.minusMonths(1);

        AttendanceRecord r1 = mock(AttendanceRecord.class); // 今月 60分
        when(r1.isUnfinished()).thenReturn(false);
        when(r1.getWorkDate()).thenReturn(thisMonth.atDay(10));
        when(r1.calculateWorkingMinutes()).thenReturn(60L);

        AttendanceRecord r2 = mock(AttendanceRecord.class); // 今月 120分
        when(r2.isUnfinished()).thenReturn(false);
        when(r2.getWorkDate()).thenReturn(thisMonth.atDay(11));
        when(r2.calculateWorkingMinutes()).thenReturn(120L);

        AttendanceRecord r3 = mock(AttendanceRecord.class); // 先月 30分
        when(r3.isUnfinished()).thenReturn(false);
        when(r3.getWorkDate()).thenReturn(lastMonth.atDay(5));
        when(r3.calculateWorkingMinutes()).thenReturn(30L);

        AttendanceRecord unfinished = mock(AttendanceRecord.class); // 未退勤 → 集計対象外
        when(unfinished.isUnfinished()).thenReturn(true);

        when(attendanceRecordRepository.findByUserIdBetweenDates(eq(target.getId()), any(), any()))
                .thenReturn(List.of(r1, r2, r3, unfinished));

        // Act
        Map<YearMonth, Long> result = adminUserService.getMonthlyWorkSummary(targetUserId);

        // Assert（集計値）
        assertEquals(2, result.size());
        assertEquals(180L, result.get(thisMonth)); // 60 + 120
        assertEquals(30L, result.get(lastMonth));

        // Assert（並び：新しい月 → 古い月）
        List<YearMonth> keys = new ArrayList<>(result.keySet());
        assertEquals(thisMonth, keys.get(0));
        assertEquals(lastMonth, keys.get(1));

        // Assert（Repository に渡した期間が「当月含む直近12か月」になっている）
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

        verify(attendanceRecordRepository).findByUserIdBetweenDates(eq(target.getId()), fromCaptor.capture(),
                toCaptor.capture());

        LocalDate actualFrom = fromCaptor.getValue();
        LocalDate actualTo = toCaptor.getValue();

        YearMonth oldestMonth = thisMonth.minusMonths(11);
        LocalDate expectedFrom = oldestMonth.atDay(1);
        LocalDate expectedTo = thisMonth.atEndOfMonth();

        assertEquals(expectedFrom, actualFrom);
        assertEquals(expectedTo, actualTo);
    }

    @Test
    void getMonthlyWorkSummary_whenNoRecords_returnsEmptyMap() {
        // Arrange
        Integer targetUserId = 1;
        User target = stubActiveTargetUser(targetUserId);

        when(attendanceRecordRepository.findByUserIdBetweenDates(eq(target.getId()), any(), any()))
                .thenReturn(List.of());

        // Act
        Map<YearMonth, Long> result = adminUserService.getMonthlyWorkSummary(targetUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMonthlyWorkSummary_whenUserNotFound_throwsBusinessException() {
        // Arrange
        when(userRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> adminUserService.getMonthlyWorkSummary(1));
    }

    // ===== getDailyWorkRecords =====

    @Test
    void getDailyWorkRecords_callsRepositoryWithLast365DaysRange_andReturnsList() {
        // Arrange
        Integer targetUserId = 1;
        User target = stubActiveTargetUser(targetUserId);

        List<AttendanceRecord> repoList = List.of(mock(AttendanceRecord.class));
        when(attendanceRecordRepository.findByUserIdBetweenDates(eq(target.getId()), any(), any()))
                .thenReturn(repoList);

        // Act
        List<AttendanceRecord> result = adminUserService.getDailyWorkRecords(targetUserId);

        // Assert（戻り値）
        assertSame(repoList, result);

        // Assert（期間：to - from = 364日 → 当日含めて365日分）
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

        verify(attendanceRecordRepository).findByUserIdBetweenDates(eq(target.getId()), fromCaptor.capture(),
                toCaptor.capture());

        long days = ChronoUnit.DAYS.between(fromCaptor.getValue(), toCaptor.getValue());
        assertEquals(364, days);
    }
}
