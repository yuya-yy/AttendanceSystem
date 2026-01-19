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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerDeleteUserTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void deleteUser_whenTryDeleteSelf_setsFlashError_andRedirectsToList_withoutCallingService() {
        // （Controllerの自分削除ガード）
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1);
        when(messageSource.getMessage("error.user.delete.self", null, locale)).thenReturn("SELF_DELETE");

        String view = adminUserController.deleteUser(1, session, ra, locale);

        assertEquals("redirect:/users/list", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("SELF_DELETE", flash.get("flashError"));

        verifyNoInteractions(adminUserService);
    }

    @Test
    void deleteUser_whenOk_callsService_andRedirectsToList() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999); // 自分以外
        when(messageSource.getMessage("info.user.delete.success", null, locale)).thenReturn("DELETE_OK");

        String view = adminUserController.deleteUser(1, session, ra, locale);

        assertEquals("redirect:/users/list", view);
        assertNull(ra.getFlashAttributes().get("flashError"));
        assertEquals("DELETE_OK", ra.getFlashAttributes().get("flashInfo"));

        verify(adminUserService).softDeleteUser(1);
        verify(messageSource).getMessage("info.user.delete.success", null, locale);
    }

    @Test
    void deleteUser_whenBusinessException_setsFlashError_andRedirectsToList() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);

        doThrow(new BusinessException("error.user.notFound"))
                .when(adminUserService).softDeleteUser(9999);

        when(messageSource.getMessage("error.user.notFound", null, locale)).thenReturn("NOT_FOUND");

        String view = adminUserController.deleteUser(9999, session, ra, locale);

        assertEquals("redirect:/users/list", view);
        assertEquals("NOT_FOUND", ra.getFlashAttributes().get("flashError"));
    }

    @Test
    void deleteUser_whenUnexpectedException_setsSystemError_andRedirectsToList() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);

        doThrow(new RuntimeException("boom"))
                .when(adminUserService).softDeleteUser(1);

        when(messageSource.getMessage("error.system.unexpected", null, locale)).thenReturn("SYSTEM_ERROR");

        String view = adminUserController.deleteUser(1, session, ra, locale);

        assertEquals("redirect:/users/list", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }
}
