package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceFindAllUsersTest {

    @Mock
    private UserRepository userRepository;

    // AdminUserService の他依存（このテストでは未使用でも @InjectMocks のために置く）
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private WorkLocationRepository workLocationRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void findAllUsers_whenRepositoryReturnsList_returnsSameList() {
        // Arrange
        User u1 = new User();
        u1.setId(1);
        User u2 = new User();
        u2.setId(2);

        List<User> expected = List.of(u1, u2);

        when(userRepository.findAllActive()).thenReturn(expected);

        // Act
        List<User> result = adminUserService.findAllUsers();

        // Assert
        assertSame(expected, result);
        verify(userRepository).findAllActive();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findAllUsers_whenRepositoryReturnsEmpty_returnsEmptyList() {
        // Arrange
        when(userRepository.findAllActive()).thenReturn(List.of());

        // Act
        List<User> result = adminUserService.findAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findAllActive();
    }
}
