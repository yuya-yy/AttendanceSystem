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
class AttendanceControllerClockOutTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AttendanceController attendanceController;

    // No.1 / No.2（正常）→ SUCCESS
    @Test
    void clockOut_whenOk_setsFlashInfoSUCCESS_andRedirects() {
        // Arrange
        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Locale locale = Locale.JAPAN;

        when(messageSource.getMessage("info.attendance.clockOut.saved", null, locale))
                .thenReturn("SUCCESS");

        // Act
        String view = attendanceController.clockOut(session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("SUCCESS", flash.get("flashInfo"));
        assertNull(flash.get("flashError"));

        verify(attendanceService).recordClockOut(1);
    }

    // （設計書にあるなら）BusinessException系 → 例： alreadyClockOut
    @Test
    void clockOut_whenBusinessException_setsFlashError_andRedirects() {
        // Arrange
        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Locale locale = Locale.JAPAN;

        doThrow(new BusinessException("error.attendance.alreadyClockOut"))
                .when(attendanceService).recordClockOut(1);

        when(messageSource.getMessage("error.attendance.alreadyClockOut", null, locale))
                .thenReturn("ERROR_ALREADY_CLOCKED_OUT");

        // Act
        String view = attendanceController.clockOut(session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("ERROR_ALREADY_CLOCKED_OUT", flash.get("flashError"));
        assertNull(flash.get("flashInfo"));
    }

    // No.5 / No.6（異常：DB取得例外、save例外）→ ERROR_SYSTEM
    @Test
    void clockOut_whenUnexpectedException_setsFlashErrorERROR_SYSTEM_andRedirects() {
        // Arrange
        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Locale locale = Locale.JAPAN;

        doThrow(new RuntimeException("db error"))
                .when(attendanceService).recordClockOut(1);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("ERROR_SYSTEM");

        // Act
        String view = attendanceController.clockOut(session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("ERROR_SYSTEM", flash.get("flashError"));
        assertNull(flash.get("flashInfo"));
    }
}
