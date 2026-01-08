package com.example.attendance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceContactInfoTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkLocationRepository workLocationRepository;
    // UserSettingService のコンストラクタ引数にある想定で置いています（このテストでは未使用）

    @InjectMocks
    private UserSettingService userSettingService;

    @Test
    void getContactInfo_whenActiveUserExists_returnsUser() {
        // Arrange
        Integer userId = 1;

        User user = new User();
        user.setId(userId);
        user.setDeletedAt(null); // 有効

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // Act
        User result = userSettingService.getContactInfo(userId);

        // Assert
        assertSame(user, result);
        verify(userRepository).findByIdAndDeletedAtIsNull(userId);
    }
}
