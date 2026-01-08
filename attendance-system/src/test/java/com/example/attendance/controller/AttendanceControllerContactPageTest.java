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
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.UserSettingService;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerContactPageTest {

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private AttendanceService attendanceService;
    // AttendanceController の他メソッド用に依存がある想定（このテストでは未使用）

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    void showContactPage_whenOk_setsContactInfoAndReturnsContact() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);

        User contactInfo = new User();
        contactInfo.setId(1);
        contactInfo.setEmail("user001@mail.com");
        contactInfo.setPhone("09012345678");

        when(userSettingService.getContactInfo(1)).thenReturn(contactInfo);

        // Act
        String view = attendanceController.showContactPage(session, model, ra, locale);

        // Assert
        assertEquals("contact", view);
        assertSame(contactInfo, model.getAttribute("contactInfo"));
        assertTrue(ra.getFlashAttributes().isEmpty());

        verify(userSettingService).getContactInfo(1);
        verifyNoInteractions(messageSource);
    }

    @Test
    void showContactPage_whenBusinessException_redirectsToAttendance_andSetsFlashError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);

        doThrow(new BusinessException("error.user.notFound"))
                .when(userSettingService).getContactInfo(1);

        when(messageSource.getMessage("error.user.notFound", null, locale))
                .thenReturn("NOT_FOUND");

        // Act
        String view = attendanceController.showContactPage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("NOT_FOUND", flash.get("flashError"));

        // contactInfo は model に入らない（tryの途中で止まるため）
        assertNull(model.getAttribute("contactInfo"));
    }

    @Test
    void showContactPage_whenUnexpectedException_redirectsToAttendance_andSetsSystemError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);

        doThrow(new RuntimeException("boom"))
                .when(userSettingService).getContactInfo(1);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // Act
        String view = attendanceController.showContactPage(session, model, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }
}
