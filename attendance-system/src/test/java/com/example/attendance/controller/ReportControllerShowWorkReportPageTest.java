package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.User;
import com.example.attendance.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class ReportControllerShowWorkReportPageTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AdminUserController reportController;

    @Test
    void showWorkReportPage_whenOk_setsModelAndReturnsWorkReport() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        Integer targetUserId = 1;

        User targetUser = new User();
        targetUser.setId(targetUserId);

        List<AttendanceRecord> dailyRecords = List.of(mock(AttendanceRecord.class));
        Map<YearMonth, Long> monthlySummary = Map.of(YearMonth.now(), 123L);

        when(adminUserService.findUserDetail(targetUserId)).thenReturn(targetUser);
        when(adminUserService.getDailyWorkRecords(targetUserId)).thenReturn(dailyRecords);
        when(adminUserService.getMonthlyWorkSummary(targetUserId)).thenReturn(monthlySummary);

        // Act
        String view = reportController.showWorkReportPage(targetUserId, ra, locale, model);

        // Assert
        assertEquals("work_report", view);
        assertSame(targetUser, model.getAttribute("targetUser"));
        assertSame(dailyRecords, model.getAttribute("dailyRecords"));
        assertSame(monthlySummary, model.getAttribute("monthlySummary"));
        assertTrue(ra.getFlashAttributes().isEmpty());

        verify(adminUserService).findUserDetail(targetUserId);
        verify(adminUserService).getDailyWorkRecords(targetUserId);
        verify(adminUserService).getMonthlyWorkSummary(targetUserId);
    }

    @Test
    void showWorkReportPage_whenBusinessException_redirectsToUsersList_setsFlashError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        Integer targetUserId = 1;

        doThrow(new BusinessException("error.user.notFound"))
                .when(adminUserService).findUserDetail(targetUserId);

        when(messageSource.getMessage("error.user.notFound", null, locale))
                .thenReturn("NOT_FOUND");

        // Act
        String view = reportController.showWorkReportPage(targetUserId, ra, locale, model);

        // Assert
        assertEquals("redirect:/users/list", view);
        assertEquals("NOT_FOUND", ra.getFlashAttributes().get("flashError"));

        // findUserDetail で落ちるので、他は呼ばれない
        verify(adminUserService).findUserDetail(targetUserId);
        verify(adminUserService, never()).getDailyWorkRecords(anyInt());
        verify(adminUserService, never()).getMonthlyWorkSummary(anyInt());
    }

    @Test
    void showWorkReportPage_whenUnexpectedException_redirectsToUsersList_setsSystemError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        Integer targetUserId = 1;

        doThrow(new RuntimeException("boom"))
                .when(adminUserService).findUserDetail(targetUserId);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // Act
        String view = reportController.showWorkReportPage(targetUserId, ra, locale, model);

        // Assert
        assertEquals("redirect:/users/list", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }
}
