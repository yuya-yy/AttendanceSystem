package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.Map;

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
class AdminUserControllerRegisterUserTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void registerUser_whenOk_setsFlashInfo_andRedirectsToList() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(messageSource.getMessage("info.user.register.success", null, locale))
                .thenReturn("REGISTER_OK");

        // Act
        String view = adminUserController.registerUser(
                "user001",
                "山田太郎",
                "Abc123!",
                "user001@mail.com",
                "090-1234-5678",
                "2", // roleValue (String)
                "10", // departmentIdValue (String)
                "", // defaultWorkLocationIdValue → parseIntegerOrNull で null想定
                ra,
                locale);

        // Assert
        assertEquals("redirect:/users/list", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("REGISTER_OK", flash.get("flashInfo"));
        assertNull(flash.get("flashError"));

        // ★ Controller側の「String → Integer変換」が正しくServiceに渡っているか
        verify(adminUserService).registerUser(
                "user001",
                "山田太郎",
                "Abc123!",
                "user001@mail.com",
                "090-1234-5678",
                2,
                10,
                null);
    }

    @Test
    void registerUser_whenBusinessException_setsFlashError_keepsInputs_andRedirectsToNew() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new BusinessException("validation.username.duplicate"))
                .when(adminUserService)
                .registerUser(any(), any(), any(), any(), any(), any(), any(), any());

        when(messageSource.getMessage("validation.username.duplicate", null, locale))
                .thenReturn("USERNAME_DUP");

        // Act
        String view = adminUserController.registerUser(
                "user001",
                "山田太郎",
                "Abc123!",
                "user001@mail.com",
                "090-1234-5678",
                "2",
                "10",
                "20",
                ra,
                locale);

        // Assert
        assertEquals("redirect:/users/new", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("USERNAME_DUP", flash.get("flashError"));

        // ★ 入力保持（画面に戻ったとき再表示用）
        assertEquals("user001", flash.get("username"));
        assertEquals("山田太郎", flash.get("displayName"));
        assertEquals("user001@mail.com", flash.get("email"));
        assertEquals("090-1234-5678", flash.get("phone"));
        assertEquals("2", flash.get("roleValue"));
        assertEquals("10", flash.get("departmentIdValue"));
        assertEquals("20", flash.get("defaultWorkLocationIdValue"));
    }

    @Test
    void registerUser_whenUnexpectedException_setsSystemError_andRedirectsToNew() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new RuntimeException("boom"))
                .when(adminUserService)
                .registerUser(any(), any(), any(), any(), any(), any(), any(), any());

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("SYSTEM_ERROR");

        // Act
        String view = adminUserController.registerUser(
                "user001",
                "山田太郎",
                "Abc123!",
                null,
                null,
                "2",
                "10",
                null,
                ra,
                locale);

        // Assert
        assertEquals("redirect:/users/new", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("SYSTEM_ERROR", flash.get("flashError"));
    }

    @Test
    void registerUser_whenRoleIsTamperedToText_parseBecomesNull_andServiceGetsNull() {
        // Arrange
        Locale locale = Locale.JAPAN;
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        // role が null になる → Service側で role必須チェックにより BusinessException が来る想定
        doThrow(new BusinessException("validation.role.required"))
                .when(adminUserService)
                .registerUser(any(), any(), any(), any(), any(), isNull(), any(), any());

        when(messageSource.getMessage("validation.role.required", null, locale))
                .thenReturn("ROLE_REQUIRED");

        // Act（roleValue に "abc" を入れる＝改ざん想定）
        String view = adminUserController.registerUser(
                "user001",
                "山田太郎",
                "Abc123!",
                null,
                null,
                "abc", // ★改ざん
                "10",
                null,
                ra,
                locale);

        // Assert
        assertEquals("redirect:/users/new", view);
        assertEquals("ROLE_REQUIRED", ra.getFlashAttributes().get("flashError"));
    }
}
