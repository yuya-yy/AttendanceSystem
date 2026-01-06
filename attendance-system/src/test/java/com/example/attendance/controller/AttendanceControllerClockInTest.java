package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.service.AttendanceService;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerClockInTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    void clockIn_whenOk_setsFlashInfo_andRedirects() {
        // Arrange
        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Locale locale = Locale.JAPAN;

        when(messageSource.getMessage("info.attendance.clockIn.saved", null, locale))
                .thenReturn("CLOCK_IN_OK");

        // Act
        String view = attendanceController.clockIn(session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("CLOCK_IN_OK", flash.get("flashInfo"));
        assertNull(flash.get("flashError"));

        verify(attendanceService).recordClockIn(1);
    }

    @Test
    void clockIn_whenBusinessException_setsFlashError_andRedirects() {
        // Arrange
        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Locale locale = Locale.JAPAN;

        doThrow(new BusinessException("error.attendance.alreadyClockIn"))
                .when(attendanceService).recordClockIn(1);

        when(messageSource.getMessage("error.attendance.alreadyClockIn", null, locale))
                .thenReturn("ALREADY_CLOCKED_IN");

        // Act
        String view = attendanceController.clockIn(session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("ALREADY_CLOCKED_IN", flash.get("flashError"));
        assertNull(flash.get("flashInfo"));
    }

    @Test
    void clockIn_whenUnexpectedException_setsSystemError_andRedirects() {
        // Arrange
        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Locale locale = Locale.JAPAN;

        doThrow(new RuntimeException("boom"))
                .when(attendanceService).recordClockIn(1);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // Act
        String view = attendanceController.clockIn(session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("SYSTEM_ERROR", flash.get("flashError"));
        assertNull(flash.get("flashInfo"));
    }
}
