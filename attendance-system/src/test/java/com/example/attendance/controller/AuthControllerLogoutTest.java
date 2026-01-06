package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendance.service.AuthService;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AuthControllerLogoutTest {

    @Mock
    private AuthService authService; // logout では使わないが、Controllerのコンストラクタ都合で用意

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @Mock
    private RedirectAttributes redirectAttributes;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, messageSource);
    }

    // No.1（正常）ログイン中：session.invalidate() され、/auth/loginへリダイレクト
    @Test
    void logout_loggedIn_invalidatesSession_andRedirectsToLogin() {
        String result = controller.logout(session, redirectAttributes, Locale.JAPANESE);

        assertEquals("redirect:/auth/login", result);

        verify(session).invalidate();
        verifyNoInteractions(messageSource);
        verify(redirectAttributes, never()).addFlashAttribute(eq("flashError"), any());
    }

    // No.2（正常）画面遷移の確認：戻り値が必ず /auth/login であること
    @Test
    void logout_alwaysRedirectsToLogin() {
        String result = controller.logout(session, redirectAttributes, Locale.JAPANESE);

        assertEquals("redirect:/auth/login", result);
    }

    // No.3（仕様上はエラーにしない）未ログイン相当（user情報なしでも）→ invalidateして /auth/login へ
    @Test
    void logout_notLoggedInLike_stillInvalidatesSession_andRedirectsToLogin() {
        // このlogout実装は userId を見ていないので、未ログイン相当でも挙動は同じ
        String result = controller.logout(session, redirectAttributes, Locale.JAPANESE);

        assertEquals("redirect:/auth/login", result);
        verify(session).invalidate();
        verify(redirectAttributes, never()).addFlashAttribute(eq("flashError"), any());
    }

    // No.4（異常）session.invalidate() が例外 → flashError を入れて /auth/login へ
    @Test
    void logout_whenSessionInvalidateThrows_setsFlashError_andRedirectsToLogin() {
        doThrow(new RuntimeException("boom")).when(session).invalidate();
        when(messageSource.getMessage(eq("error.system.unexpected"), isNull(), eq(Locale.JAPANESE)))
                .thenReturn("システムエラーが発生しました。");

        String result = controller.logout(session, redirectAttributes, Locale.JAPANESE);

        assertEquals("redirect:/auth/login", result);

        verify(session).invalidate();
        verify(messageSource).getMessage(eq("error.system.unexpected"), isNull(), eq(Locale.JAPANESE));
        verify(redirectAttributes).addFlashAttribute("flashError", "システムエラーが発生しました。");
    }

    // No.5（境界）session が null でも落ちずに /auth/login へ
    @Test
    void logout_whenSessionIsNull_doesNotThrow_andRedirectsToLogin() {
        String result = controller.logout(null, redirectAttributes, Locale.JAPANESE);

        assertEquals("redirect:/auth/login", result);

        verifyNoInteractions(messageSource);
        verifyNoInteractions(redirectAttributes); // 例外が起きない限り flashError も入らない
    }
}
