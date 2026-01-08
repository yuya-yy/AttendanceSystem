package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.example.attendance.entity.Department;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerShowUserNewPageTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void showUserNewPage_whenOk_setsDepartmentsAndWorkLocations_andReturnsViewName() {
        // Arrange
        Model model = new ConcurrentModel();

        Department d1 = new Department();
        d1.setId(10);
        Department d2 = new Department();
        d2.setId(20);

        WorkLocation w1 = new WorkLocation();
        w1.setId(1);
        WorkLocation w2 = new WorkLocation();
        w2.setId(2);

        List<Department> departments = List.of(d1, d2);
        List<WorkLocation> workLocations = List.of(w1, w2);

        when(adminUserService.getActiveDepartments()).thenReturn(departments);
        when(adminUserService.getActiveWorkLocations()).thenReturn(workLocations);

        // Act
        String view = adminUserController.showUserNewPage(model);

        // Assert
        assertEquals("user_new", view);

        assertSame(departments, model.getAttribute("departments"));
        assertSame(workLocations, model.getAttribute("workLocations"));

        verify(adminUserService).getActiveDepartments();
        verify(adminUserService).getActiveWorkLocations();
        verifyNoMoreInteractions(adminUserService);
    }
}
