package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceUserEditSupportTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    // AdminUserService のコンストラクタ引数にある前提で置く（このテストでは未使用でもOK）
    @Mock
    private WorkLocationRepository workLocationRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void findUserDetail_whenActiveUserExists_returnsUser() {
        // Arrange
        Integer userId = 1;
        User user = new User();
        user.setId(userId);
        user.setDeletedAt(null);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // Act
        User result = adminUserService.findUserDetail(userId);

        // Assert
        assertSame(user, result);
        verify(userRepository).findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    void findUserDetail_whenNotFound_throwsBusinessException() {
        // Arrange
        Integer userId = 1;
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.findUserDetail(userId));

        // requireActiveTargetUser の仕様に合わせてください（あなたの実装が notFound ならこのまま）
        assertEquals("error.user.notFound", ex.getMessageKey());
        verify(userRepository).findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    void findAllActiveDepartments_returnsListFromRepository() {
        // Arrange
        Department d1 = new Department();
        d1.setId(10);
        Department d2 = new Department();
        d2.setId(20);

        List<Department> list = List.of(d1, d2);
        when(departmentRepository.findAllActive()).thenReturn(list);

        // Act
        List<Department> result = adminUserService.findAllActiveDepartments();

        // Assert
        assertSame(list, result);
        verify(departmentRepository).findAllActive();
    }
}
