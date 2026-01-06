package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void authenticate_usernameIsNull_throwsBusinessException() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate(null, "password"));

        assertEquals("error.auth.username.required", ex.getMessageKey());

        // 入力チェックで落ちるので、DBアクセス等は発生しない
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void authenticate_usernameIsBlank_throwsBusinessException() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate("   ", "password"));

        assertEquals("error.auth.username.required", ex.getMessageKey());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void authenticate_passwordIsNull_throwsBusinessException() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate("testuser", null));

        assertEquals("error.auth.password.required", ex.getMessageKey());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void authenticate_passwordIsBlank_throwsBusinessException() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate("testuser", "   "));

        assertEquals("error.auth.password.required", ex.getMessageKey());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void authenticate_userNotFound_throwsLoginFailed() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate("testuser", "password"));

        assertEquals("error.auth.loginFailed", ex.getMessageKey());

        verify(userRepository).findByUsername("testuser");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void authenticate_passwordMismatch_throwsLoginFailed() {
        User user = createUser(1, "testuser", "テストユーザー", "hashed");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate("testuser", "password"));

        assertEquals("error.auth.loginFailed", ex.getMessageKey());

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password", "hashed");
    }

    @Test
    void authenticate_passwordHashIsNull_throwsLoginFailed_withoutCallingEncoder() {
        User user = createUser(1, "testuser", "テストユーザー", null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> authService.authenticate("testuser", "password"));

        assertEquals("error.auth.loginFailed", ex.getMessageKey());

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticate_success_returnsUser_andTrimsUsername() {
        User user = createUser(1, "testuser", "テストユーザー", "hashed");

        // ★ username は trim() されるので "testuser" で stub する
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        User result = authService.authenticate("  testuser  ", "password");

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("テストユーザー", result.getDisplayName());

        verify(userRepository).findByUsername("testuser"); // trim確認
        verify(passwordEncoder).matches("password", "hashed");
    }

    private static User createUser(Integer id, String username, String displayName, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordHash);
        return user;
    }
}
