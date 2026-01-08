package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// ↓ AuthController が依存している型に合わせて import を追加/調整
import com.example.attendance.service.AuthService;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class AuthControllerShowLoginPageTest {

    // ↓ AuthController の依存に合わせて増減する
    @Mock
    private AuthService authService;

    @Mock
    private MessageSource messageSource;

    // Mockitoが「モックを渡して」AuthControllerをnewしてくれる
    @InjectMocks
    private AuthController authController;

    @Test
    void showLoginPage_returnsLoginViewName() {
        // Act
        String viewName = authController.showLoginPage();

        // Assert
        assertEquals("login", viewName);
    }
}
