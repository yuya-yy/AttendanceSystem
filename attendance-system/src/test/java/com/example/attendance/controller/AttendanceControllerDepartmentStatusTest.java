package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Locale;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.service.AttendanceService;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerDepartmentStatusTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    void showDepartmentStatusPage_whenDepartmentIdMissing_redirectsToAttendance_withSystemError_andDoesNotCallService() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("departmentId")).thenReturn(null);
        when(messageSource.getMessage("error.user.department.setting", null, locale)).thenReturn("SYSTEM_ERROR");

        // Act
        String view = attendanceController.showDepartmentStatusPage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
        verifyNoInteractions(attendanceService);
    }

    @Test
    void showDepartmentStatusPage_whenOk_setsStatusToModel_andReturnsStatusList() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("departmentId")).thenReturn(10);

        AttendanceService.DepartmentStatusView status = new AttendanceService.DepartmentStatusView("開発部",
                java.util.List.of(), java.util.Set.of());

        when(attendanceService.getDepartmentCurrentStatus(10)).thenReturn(status);

        // Act
        String view = attendanceController.showDepartmentStatusPage(session, model, ra, locale);

        // Assert
        assertEquals("status_list", view);
        assertSame(status, model.getAttribute("status"));
        assertTrue(ra.getFlashAttributes().isEmpty());

        verify(attendanceService).getDepartmentCurrentStatus(10);
    }

    @Test
    void showDepartmentStatusPage_whenBusinessException_redirectsToAttendance_andSetsFlashError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("departmentId")).thenReturn(10);

        doThrow(new BusinessException("error.user.department.notFound"))
                .when(attendanceService).getDepartmentCurrentStatus(10);

        when(messageSource.getMessage("error.user.department.notFound", null, locale))
                .thenReturn("DEP_NOT_FOUND");

        // Act
        String view = attendanceController.showDepartmentStatusPage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        assertEquals("DEP_NOT_FOUND", ra.getFlashAttributes().get("flashError"));
    }

    @Test
    void showDepartmentStatusPage_whenUnexpectedException_redirectsToAttendance_andSetsSystemError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("departmentId")).thenReturn(10);

        doThrow(new RuntimeException("boom"))
                .when(attendanceService).getDepartmentCurrentStatus(10);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // Act
        String view = attendanceController.showDepartmentStatusPage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }
}
