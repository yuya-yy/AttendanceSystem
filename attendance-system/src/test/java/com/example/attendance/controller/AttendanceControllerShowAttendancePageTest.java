package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.service.AttendanceService;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerShowAttendancePageTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    void showAttendancePage_whenWorkingNow_returnsAttendance_andSetsModelAsWorking() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);
        when(session.getAttribute("role")).thenReturn(2);

        User loginUser = new User();
        loginUser.setId(1);
        loginUser.setDisplayName("山田太郎");

        Department dept = new Department();
        dept.setDepartmentName("開発部");
        loginUser.setDepartment(dept);

        when(attendanceService.requireActiveCurrentUser(1)).thenReturn(loginUser);

        AttendanceRecord latest = new AttendanceRecord(); // 未退勤あり＝出勤中
        when(attendanceService.findLatestUnfinished(1)).thenReturn(latest);

        when(attendanceService.getRecentRecords(1)).thenReturn(List.of(new AttendanceRecord()));
        when(attendanceService.getCurrentWorkLocationName(1)).thenReturn("会社");

        // Act
        String view = attendanceController.showAttendancePage(session, model, ra, locale);

        // Assert
        assertEquals("attendance", view);

        assertEquals("山田太郎", model.getAttribute("displayName"));
        assertEquals("開発部", model.getAttribute("departmentName"));
        assertEquals("一般", model.getAttribute("roleLabel"));

        assertEquals(true, model.getAttribute("workingNow"));
        assertEquals("出勤中", model.getAttribute("workStatusLabel"));

        assertSame(latest, model.getAttribute("latestRecord"));
        assertEquals("会社", model.getAttribute("currentWorkLocationName"));
        assertNotNull(model.getAttribute("recentRecords"));

        // 日付・時刻は「値が入っていること」までにする（完全一致は壊れやすい）
        assertNotNull(model.getAttribute("todayDateText"));
        assertNotNull(model.getAttribute("nowTimeText"));

        Object serverEpochMillis = model.getAttribute("serverEpochMillis");
        assertNotNull(serverEpochMillis);
        assertTrue(serverEpochMillis instanceof Long);
    }

    @Test
    void showAttendancePage_whenNotWorking_returnsAttendance_andSetsModelAsNotWorking() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);
        when(session.getAttribute("role")).thenReturn(2);

        User loginUser = new User();
        loginUser.setId(1);
        loginUser.setDisplayName("山田太郎");
        when(attendanceService.requireActiveCurrentUser(1)).thenReturn(loginUser);

        // 未退勤なし＝勤務外
        when(attendanceService.findLatestUnfinished(1)).thenReturn(null);

        when(attendanceService.getRecentRecords(1)).thenReturn(List.of());
        when(attendanceService.getCurrentWorkLocationName(1)).thenReturn("未設定");

        // Act
        String view = attendanceController.showAttendancePage(session, model, ra, locale);

        // Assert
        assertEquals("attendance", view);
        assertEquals(false, model.getAttribute("workingNow"));
        assertEquals("勤務外", model.getAttribute("workStatusLabel"));
        assertNull(model.getAttribute("latestRecord"));
    }

    @Test
    void showAttendancePage_whenBusinessException_redirectsToLogin_andSetsFlashError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);
        when(session.getAttribute("role")).thenReturn(2);

        doThrow(new BusinessException("error.auth.required"))
                .when(attendanceService).requireActiveCurrentUser(1);

        when(messageSource.getMessage("error.auth.required", null, locale))
                .thenReturn("LOGIN_REQUIRED");

        // Act
        String view = attendanceController.showAttendancePage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/auth/login", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("LOGIN_REQUIRED", flash.get("flashError"));
    }

    @Test
    void showAttendancePage_whenUnexpectedException_redirectsToLogin_andSetsSystemError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);
        when(session.getAttribute("role")).thenReturn(2);

        doThrow(new RuntimeException("boom"))
                .when(attendanceService).requireActiveCurrentUser(1);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // Act
        String view = attendanceController.showAttendancePage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/auth/login", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }

    @Test
    void showAttendancePage_whenDepartmentIsNull_doesNotCrash_andSetsDepartmentNameEmpty() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        when(session.getAttribute("role")).thenReturn(2);
        when(session.getAttribute("userId")).thenReturn(1);

        User loginUser = new User();
        loginUser.setId(1);
        loginUser.setDisplayName("山田太郎");
        loginUser.setDepartment(null); // ★ここがポイント

        when(attendanceService.requireActiveCurrentUser(1)).thenReturn(loginUser);
        when(attendanceService.findLatestUnfinished(1)).thenReturn(null);
        when(attendanceService.getRecentRecords(1)).thenReturn(List.of());
        when(attendanceService.getCurrentWorkLocationName(1)).thenReturn("未設定");

        // Act
        String view = attendanceController.showAttendancePage(session, model, ra, locale);

        // Assert
        assertEquals("attendance", view);

        assertEquals("山田太郎", model.getAttribute("displayName"));
        assertEquals("", model.getAttribute("departmentName")); // ★nullなら空文字
        assertEquals("一般", model.getAttribute("roleLabel"));

        assertEquals(false, model.getAttribute("workingNow"));
        assertEquals("勤務外", model.getAttribute("workStatusLabel"));

        // recentRecords が入っていること（空でもOK）
        assertNotNull(model.getAttribute("recentRecords"));

        // エラーリダイレクトになっていないこと
        assertTrue(ra.getFlashAttributes().isEmpty());
    }
}
