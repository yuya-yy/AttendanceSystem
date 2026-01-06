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
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerExecuteLoginTest {

    @Mock
    private AuthService authService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AuthController authController;

    @Test
    void executeLogin_whenSuccess_setsSession_andRedirectsToAttendance_withDepartmentId() {
        // Arrange
        String username = "testuser";
        String password = "password";
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        // User / Department は本物のEntityを使っています
        User user = new User();
        user.setId(1);
        user.setDisplayName("表示名");
        user.setRole(2);

        Department dept = new Department();
        dept.setId(10);
        user.setDepartment(dept);

        when(authService.authenticate(username, password)).thenReturn(user);

        // Act
        String view = authController.executeLogin(username, password, session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);

        assertEquals(1, session.getAttribute("userId"));
        assertEquals("表示名", session.getAttribute("displayName"));
        assertEquals(2, session.getAttribute("role"));
        assertEquals(10, session.getAttribute("departmentId"));

        // 成功時は flashError を入れない想定
        Map<String, ?> flash = ra.getFlashAttributes();
        assertNull(flash.get("flashError"));

        verify(authService).authenticate(username, password);
    }

    @Test
    void executeLogin_whenSuccess_withoutDepartment_doesNotSetDepartmentId() {
        // Arrange
        String username = "testuser";
        String password = "password";
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        User user = new User();
        user.setId(1);
        user.setDisplayName("表示名");
        user.setRole(2);
        user.setDepartment(null); // ★ 部署なし

        when(authService.authenticate(username, password)).thenReturn(user);

        // Act
        String view = authController.executeLogin(username, password, session, ra, locale);

        // Assert
        assertEquals("redirect:/attendance", view);
        assertNull(session.getAttribute("departmentId"));
    }

    @Test
    void executeLogin_whenBusinessException_passwordRequired_setsFlashError_andKeepsUsername() {
        // Arrange
        String username = "testuser";
        String password = ""; // 空欄想定
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new BusinessException("error.auth.password.required"))
                .when(authService).authenticate(username, password);

        when(messageSource.getMessage("error.auth.password.required", null, locale))
                .thenReturn("パスワードは必須です");

        // Act
        String view = authController.executeLogin(username, password, session, ra, locale);

        // Assert
        assertEquals("redirect:/auth/login", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("パスワードは必須です", flash.get("flashError"));
        // ★ パスワード空欄のときだけ username を保持
        assertEquals(username, flash.get("loginUsername"));

        // セッションは成功時のみ保存するので、失敗時は入っていない想定
        assertNull(session.getAttribute("userId"));
    }

    @Test
    void executeLogin_whenBusinessException_loginFailed_setsFlashError_butDoesNotKeepUsername() {
        // Arrange
        String username = "testuser";
        String password = "wrong";
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new BusinessException("error.auth.loginFailed"))
                .when(authService).authenticate(username, password);

        when(messageSource.getMessage("error.auth.loginFailed", null, locale))
                .thenReturn("ユーザー名またはパスワードが違います");

        // Act
        String view = authController.executeLogin(username, password, session, ra, locale);

        // Assert
        assertEquals("redirect:/auth/login", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("ユーザー名またはパスワードが違います", flash.get("flashError"));
        // ★ loginFailed のときは username を保持しない（空欄に戻す）
        assertNull(flash.get("loginUsername"));
    }

    @Test
    void executeLogin_whenUnexpectedException_setsSystemFlashError_andRedirectsToLogin() {
        // Arrange
        String username = "testuser";
        String password = "password";
        Locale locale = Locale.JAPAN;

        HttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        doThrow(new RuntimeException("boom"))
                .when(authService).authenticate(username, password);

        when(messageSource.getMessage("error.system.unexpected", null, locale))
                .thenReturn("システムエラーが発生しました");

        // Act
        String view = authController.executeLogin(username, password, session, ra, locale);

        // Assert
        assertEquals("redirect:/auth/login", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("システムエラーが発生しました", flash.get("flashError"));
    }
}
