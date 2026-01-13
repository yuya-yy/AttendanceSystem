package com.example.attendance.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @Test
    void preHandle_publicPath_allowsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void preHandle_noSession_redirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/attendance");
        request.addPreferredLocale(Locale.JAPAN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(messageSource.getMessage(eq("error.auth.required"), isNull(), any(Locale.class)))
                .thenReturn("AUTH_REQUIRED");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertTrue(response.getRedirectedUrl().endsWith("/auth/login"));
    }

    @Test
    void preHandle_deletedUser_redirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/attendance");
        request.addPreferredLocale(Locale.JAPAN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);
        request.setSession(session);

        when(userRepository.existsByIdAndDeletedAtIsNull(1)).thenReturn(false);
        when(messageSource.getMessage(eq("error.auth.userDeleted"), isNull(), any(Locale.class)))
                .thenReturn("USER_DELETED");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertTrue(response.getRedirectedUrl().endsWith("/auth/login"));
    }

    @Test
    void preHandle_adminOnlyPath_nonAdmin_redirectsToAttendance() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/users/new");
        request.addPreferredLocale(Locale.JAPAN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);
        session.setAttribute("role", 2);
        request.setSession(session);

        when(userRepository.existsByIdAndDeletedAtIsNull(1)).thenReturn(true);
        when(messageSource.getMessage(eq("error.auth.forbidden"), isNull(), any(Locale.class)))
                .thenReturn("FORBIDDEN");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertTrue(response.getRedirectedUrl().endsWith("/attendance"));
    }

    @Test
    void preHandle_adminOnlyPath_admin_allowsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/users/new");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);
        session.setAttribute("role", 1);
        request.setSession(session);

        when(userRepository.existsByIdAndDeletedAtIsNull(1)).thenReturn(true);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void preHandle_normalPath_userOk_allowsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/attendance");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1);
        session.setAttribute("role", 2);
        request.setSession(session);

        when(userRepository.existsByIdAndDeletedAtIsNull(1)).thenReturn(true);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }
}
