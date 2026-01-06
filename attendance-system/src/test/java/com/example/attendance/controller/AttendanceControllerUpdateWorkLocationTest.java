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
import com.example.attendance.service.UserSettingService;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerUpdateWorkLocationTest {

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    void updateWorkLocation_whenOk_setsFlashInfo_andRedirects() {
        // Arrange
        Integer workLocationId = 10;
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(messageSource.getMessage("info.workLocation.saved", null, locale))
                .thenReturn("SUCCESS");

        // Act
        String view = attendanceController.updateWorkLocation(workLocationId, session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance/work-location", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("SUCCESS", flash.get("flashInfo"));
        assertNull(flash.get("flashError"));

        verify(userSettingService).updateWorkLocation(1, 10);
    }

    @Test
    void updateWorkLocation_whenBusinessException_setsFlashError_andRedirects() {
        // Arrange
        Integer workLocationId = null; // 未選択ケースなどを想定
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new BusinessException("error.workLocation.required"))
                .when(userSettingService).updateWorkLocation(1, null);

        when(messageSource.getMessage("error.workLocation.required", null, locale))
                .thenReturn("ERROR_WORK_LOCATION_REQUIRED");

        // Act
        String view = attendanceController.updateWorkLocation(workLocationId, session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance/work-location", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("ERROR_WORK_LOCATION_REQUIRED", flash.get("flashError"));
        assertNull(flash.get("flashInfo"));
    }

    @Test
    void updateWorkLocation_whenUnexpectedException_setsSystemError_andRedirects() {
        // Arrange
        Integer workLocationId = 10;
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new RuntimeException("boom"))
                .when(userSettingService).updateWorkLocation(1, 10);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("ERROR_SYSTEM");

        // Act
        String view = attendanceController.updateWorkLocation(workLocationId, session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance/work-location", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("ERROR_SYSTEM", flash.get("flashError"));
        assertNull(flash.get("flashInfo"));
    }
}
