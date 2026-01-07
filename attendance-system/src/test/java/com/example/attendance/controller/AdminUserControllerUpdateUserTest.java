package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
class AdminUserControllerUpdateUserTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void updateUser_whenOk_setsFlashInfo_andRedirectsToList() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999); // 自分以外を編集
        when(messageSource.getMessage("info.user.update.success", null, locale)).thenReturn("UPDATE_OK");

        String view = adminUserController.updateUser(
                1,
                "user001",
                "山田太郎2",
                "user001@mail.com",
                "090-1234-5678",
                "10",
                "2",
                "", // password空欄
                session,
                ra,
                locale);

        assertEquals("redirect:/users/list", view);
        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("UPDATE_OK", flash.get("flashInfo"));
        assertNull(flash.get("flashError"));

        verify(adminUserService).updateUser(
                1,
                "user001",
                "山田太郎2",
                "user001@mail.com",
                "090-1234-5678",
                10,
                2,
                "");
    }

    @Test
    void updateUser_whenBusinessException_setsFlashError_keepsInputs_andRedirectsToEdit() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);

        doThrow(new BusinessException("validation.username.duplicate"))
                .when(adminUserService)
                .updateUser(anyInt(), any(), any(), any(), any(), any(), any(), any());

        when(messageSource.getMessage("validation.username.duplicate", null, locale))
                .thenReturn("USERNAME_DUP");

        String view = adminUserController.updateUser(
                1,
                "user001",
                "山田太郎",
                "user001@mail.com",
                "090-1234-5678",
                "10",
                "2",
                "",
                session,
                ra,
                locale);

        assertEquals("redirect:/users/1/edit", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("USERNAME_DUP", flash.get("flashError"));
        assertEquals("user001", flash.get("usernameValue"));
        assertEquals("山田太郎", flash.get("displayNameValue"));
        assertEquals("user001@mail.com", flash.get("emailValue"));
        assertEquals("090-1234-5678", flash.get("phoneValue"));
        assertEquals("10", flash.get("departmentIdValue"));
        assertEquals("2", flash.get("roleValue"));
    }

    @Test
    void updateUser_whenUnexpectedException_setsSystemError_andRedirectsToEdit() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);

        doThrow(new RuntimeException("boom"))
                .when(adminUserService)
                .updateUser(anyInt(), any(), any(), any(), any(), any(), any(), any());

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        String view = adminUserController.updateUser(
                1,
                "user001",
                "山田太郎",
                null,
                null,
                "10",
                "2",
                null,
                session,
                ra,
                locale);

        assertEquals("redirect:/users/1/edit", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }

    @Test
    void updateUser_whenAdminTriesToDemoteSelf_throwsProtected_andRedirectsToEdit_withoutCallingService() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        // 自分自身を編集
        when(session.getAttribute("userId")).thenReturn(1);

        when(messageSource.getMessage("error.user.role.protected", null, locale))
                .thenReturn("ROLE_PROTECTED");

        String view = adminUserController.updateUser(
                1,
                "user001",
                "山田太郎",
                null,
                null,
                "10",
                "2", // ★自分を一般に落とす
                null,
                session,
                ra,
                locale);

        assertEquals("redirect:/users/1/edit", view);
        assertEquals("ROLE_PROTECTED", ra.getFlashAttributes().get("flashError"));

        // Serviceは呼ばれない
        verifyNoInteractions(adminUserService);
    }

    @Test
    void updateUser_whenRoleIsTamperedToText_becomesNumberFormatException_andShowsSystemError() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);
        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // roleStr が "abc" → Integer.valueOf で NumberFormatException → catch(Exception)
        // へ
        String view = adminUserController.updateUser(
                1,
                "user001",
                "山田太郎",
                null,
                null,
                "10",
                "abc", // ★改ざん
                null,
                session,
                ra,
                locale);

        assertEquals("redirect:/users/1/edit", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
        verifyNoInteractions(adminUserService);
    }

    @Test
    void updateUser_whenDepartmentIdIsTamperedToText_becomesNumberFormatException_andShowsSystemError() {
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);
        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        String view = adminUserController.updateUser(
                1,
                "user001",
                "山田太郎",
                null,
                null,
                "abc", // ★改ざん
                "2",
                null,
                session,
                ra,
                locale);

        assertEquals("redirect:/users/1/edit", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
        verifyNoInteractions(adminUserService);
    }
}
