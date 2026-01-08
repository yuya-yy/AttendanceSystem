package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import com.example.attendance.entity.WorkLocation;
import com.example.attendance.service.UserSettingService;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerShowWorkLocationPageTest {

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    void showWorkLocationPage_whenCalled_setsWorkLocationsAndSelectedId_andReturnsWorkplace() {
        // Arrange（入力の準備）
        Integer userId = 1;

        when(session.getAttribute("userId")).thenReturn(userId);

        List<WorkLocation> locations = List.of(new WorkLocation(), new WorkLocation());
        when(userSettingService.getActiveWorkLocations()).thenReturn(locations);

        Integer selectedId = 10;
        when(userSettingService.getCurrentWorkLocationId(userId)).thenReturn(selectedId);

        // Act（実行）
        String view = attendanceController.showWorkLocationPage(session, model);

        // Assert（出力の確認）
        assertEquals("workplace", view);

        verify(model).addAttribute("workLocations", locations);
        verify(model).addAttribute("selectedWorkLocationId", selectedId);

        verify(userSettingService).getActiveWorkLocations();
        verify(userSettingService).getCurrentWorkLocationId(userId);
    }

    @Test
    void showWorkLocationPage_whenNoSelectedWorkLocation_setsNullSelectedId_andReturnsWorkplace() {
        // Arrange
        Integer userId = 1;

        when(session.getAttribute("userId")).thenReturn(userId);

        List<WorkLocation> locations = List.of(new WorkLocation());
        when(userSettingService.getActiveWorkLocations()).thenReturn(locations);

        // 勤務場所が未設定の想定（nullが返る）
        when(userSettingService.getCurrentWorkLocationId(userId)).thenReturn(null);

        // Act
        String view = attendanceController.showWorkLocationPage(session, model);

        // Assert
        assertEquals("workplace", view);

        verify(model).addAttribute("workLocations", locations);
        verify(model).addAttribute("selectedWorkLocationId", null);
    }
}
