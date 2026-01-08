package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerShowUserListPageTest {

    @Mock
    private AdminUserService adminUserService;

    // AdminUserController の依存に messageSource がある想定（このメソッドでは未使用でも置く）
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void showUserListPage_whenOk_setsUsersAndDepartments_andReturnsViewName() {
        // Arrange
        Model model = new ConcurrentModel();

        User u1 = new User();
        u1.setId(1);
        User u2 = new User();
        u2.setId(2);

        Department d1 = new Department();
        d1.setId(10);
        Department d2 = new Department();
        d2.setId(20);

        List<User> users = List.of(u1, u2);
        List<Department> departments = List.of(d1, d2);

        when(adminUserService.findAllUsers()).thenReturn(users);
        when(adminUserService.getActiveDepartments()).thenReturn(departments);

        // Act
        String view = adminUserController.showUserListPage(model);

        // Assert
        assertEquals("user_list", view);
        assertSame(users, model.getAttribute("users"));
        assertSame(departments, model.getAttribute("departments"));

        verify(adminUserService).findAllUsers();
        verify(adminUserService).getActiveDepartments();
        verifyNoMoreInteractions(adminUserService);
    }
}
