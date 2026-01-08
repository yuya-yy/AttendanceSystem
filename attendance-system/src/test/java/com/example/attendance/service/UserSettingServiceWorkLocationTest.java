package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceWorkLocationTest {

    @Mock
    private WorkLocationRepository workLocationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingService userSettingService;

    // ===== getActiveWorkLocations() =====

    @Test
    void getActiveWorkLocations_returnsRepositoryResult() {
        // Arrange
        WorkLocation wl1 = new WorkLocation();
        wl1.setId(1);
        wl1.setLocationName("会社");

        WorkLocation wl2 = new WorkLocation();
        wl2.setId(2);
        wl2.setLocationName("在宅");

        when(workLocationRepository.findAllActive()).thenReturn(List.of(wl1, wl2));

        // Act
        List<WorkLocation> result = userSettingService.getActiveWorkLocations();

        // Assert
        assertEquals(2, result.size());
        assertSame(wl1, result.get(0));
        assertSame(wl2, result.get(1));
        verify(workLocationRepository).findAllActive();
        verifyNoInteractions(userRepository);
    }

    // ===== getCurrentWorkLocationId(userId) =====

    @Test
    void getCurrentWorkLocationId_whenUserNotFound_returnsNull() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act
        Integer result = userSettingService.getCurrentWorkLocationId(1);

        // Assert
        assertNull(result);
        verify(userRepository).findById(1);
        verifyNoInteractions(workLocationRepository);
    }

    @Test
    void getCurrentWorkLocationId_whenUserDeleted_returnsNull() {
        // Arrange
        User user = new User();
        user.setId(1);
        user.setDeletedAt(OffsetDateTime.now()); // 論理削除扱い（isActive=false想定）

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        Integer result = userSettingService.getCurrentWorkLocationId(1);

        // Assert
        assertNull(result);
        verify(userRepository).findById(1);
        verifyNoInteractions(workLocationRepository);
    }

    @Test
    void getCurrentWorkLocationId_whenDefaultWorkLocationNull_returnsNull() {
        // Arrange
        User user = new User();
        user.setId(1);
        user.setDeletedAt(null); // 有効（isActive=true想定）
        user.setDefaultWorkLocation(null); // 未設定

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        Integer result = userSettingService.getCurrentWorkLocationId(1);

        // Assert
        assertNull(result);
        verify(userRepository).findById(1);
        verifyNoInteractions(workLocationRepository);
    }

    @Test
    void getCurrentWorkLocationId_whenDefaultWorkLocationExists_returnsId() {
        // Arrange
        WorkLocation loc = new WorkLocation();
        loc.setId(10);
        loc.setLocationName("会社");

        User user = new User();
        user.setId(1);
        user.setDeletedAt(null); // 有効（isActive=true想定）
        user.setDefaultWorkLocation(loc);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        Integer result = userSettingService.getCurrentWorkLocationId(1);

        // Assert
        assertEquals(10, result);
        verify(userRepository).findById(1);
        verifyNoInteractions(workLocationRepository);
    }
}
