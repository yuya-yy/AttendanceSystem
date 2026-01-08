package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceDepartmentStatusTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    // AttendanceService の依存にある想定（このテストでは未使用でも @InjectMocks のために置く）
    @Mock
    private WorkLocationRepository workLocationRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    // ===== テスト用ヘルパー =====

    private static User user(int id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private static AttendanceRecord recordWithUser(User u) {
        AttendanceRecord r = new AttendanceRecord();
        r.setUser(u); // setter がある前提（これまでのあなたのテストと同じ書き方）
        return r;
    }

    private static Department department(int id, String name) {
        Department d = new Department();
        d.setId(id);
        d.setDepartmentName(name);
        return d;
    }

    @Test
    void getDepartmentCurrentStatus_whenOk_sortsWorkingFirstThenIdAsc_andBuildsWorkingIds() {
        // Arrange
        Integer departmentId = 10;

        Department dept = department(departmentId, "開発部");
        when(departmentRepository.findByIdAndDeletedAtIsNull(departmentId))
                .thenReturn(Optional.of(dept));

        // users はわざとバラバラ順で返す
        User u3 = user(3);
        User u1 = user(1);
        User u2 = user(2);
        when(userRepository.findActiveByDepartmentId(departmentId))
                .thenReturn(new java.util.ArrayList<>(java.util.List.of(u3, u1, u2)));

        // 未退勤（勤務中）は userId=2 のみ + nullユーザー混入（NPE防止の filter の確認）
        when(attendanceRecordRepository.findUnfinishedByDepartmentId(departmentId))
                .thenReturn(List.of(
                        recordWithUser(u2),
                        recordWithUser(null) // ★ null は filter(Objects::nonNull) で捨てられる想定
                ));

        // Act
        AttendanceService.DepartmentStatusView view = attendanceService.getDepartmentCurrentStatus(departmentId);

        // Assert
        assertEquals("開発部", view.departmentName());
        assertEquals(Set.of(2), view.workingUserIds());

        // ソート：勤務中 → 勤務外 → ID昇順
        List<User> sorted = view.users();
        assertEquals(2, sorted.get(0).getId()); // 勤務中が先
        assertEquals(1, sorted.get(1).getId());
        assertEquals(3, sorted.get(2).getId());

        // 箱のメソッド isWorking の確認
        assertTrue(view.isWorking(2));
        assertFalse(view.isWorking(1));

        verify(departmentRepository).findByIdAndDeletedAtIsNull(departmentId);
        verify(userRepository).findActiveByDepartmentId(departmentId);
        verify(attendanceRecordRepository).findUnfinishedByDepartmentId(departmentId);
    }

    @Test
    void getDepartmentCurrentStatus_whenDepartmentNotFound_throwsBusinessException() {
        // Arrange
        Integer departmentId = 999;
        when(departmentRepository.findByIdAndDeletedAtIsNull(departmentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> attendanceService.getDepartmentCurrentStatus(departmentId));

        assertEquals("error.user.department.notFound", ex.getMessageKey());

        // 部署が無ければ後続のDBアクセスはしない
        verify(userRepository, never()).findActiveByDepartmentId(any());
        verify(attendanceRecordRepository, never()).findUnfinishedByDepartmentId(any());
    }

    @Test
    void getDepartmentCurrentStatus_whenMultipleWorkingUsers_sortsWorkingUsersByIdAsc() {
        // Arrange
        Integer departmentId = 10;

        when(departmentRepository.findByIdAndDeletedAtIsNull(departmentId))
                .thenReturn(Optional.of(department(departmentId, "開発部")));

        User u3 = user(3);
        User u2 = user(2);
        User u1 = user(1);

        when(userRepository.findActiveByDepartmentId(departmentId))
                .thenReturn(new java.util.ArrayList<>(java.util.List.of(u2, u3, u1)));

        // 勤務中：1と3
        when(attendanceRecordRepository.findUnfinishedByDepartmentId(departmentId))
                .thenReturn(List.of(recordWithUser(u3), recordWithUser(u1)));

        // Act
        AttendanceService.DepartmentStatusView view = attendanceService.getDepartmentCurrentStatus(departmentId);

        // Assert（勤務中の 1,3 が先で、ID昇順）
        assertEquals(1, view.users().get(0).getId());
        assertEquals(3, view.users().get(1).getId());
        assertEquals(2, view.users().get(2).getId());
    }
}
