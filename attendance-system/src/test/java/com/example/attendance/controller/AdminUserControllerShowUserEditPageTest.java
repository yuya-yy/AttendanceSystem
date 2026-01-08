package com.example.attendance.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.Department;
import com.example.attendance.entity.User;
import com.example.attendance.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerShowUserEditPageTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void showUserEditPage_whenOk_setsDefaultFormValues_andReturnsUserEdit() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Integer targetUserId = 1;

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999); // 自分以外を編集

        Department dept = new Department();
        dept.setId(10);

        User editUser = new User();
        editUser.setId(targetUserId);
        editUser.setUsername("user001");
        editUser.setDisplayName("山田太郎");
        editUser.setEmail("user001@mail.com");
        editUser.setPhone("090-1234-5678");
        editUser.setDepartment(dept);
        editUser.setRole(2);

        when(adminUserService.findUserDetail(targetUserId)).thenReturn(editUser);

        Department d1 = new Department();
        d1.setId(10);
        when(adminUserService.findAllActiveDepartments()).thenReturn(List.of(d1));

        // Act
        String view = adminUserController.showUserEditPage(
                targetUserId, session, model, ra, locale);

        // Assert
        assertEquals("user_edit", view);

        assertSame(editUser, model.getAttribute("editUser"));
        assertNotNull(model.getAttribute("departments"));

        assertEquals(false, model.getAttribute("isSelf"));

        // フォーム値（フラッシュが無い想定なのでDB値が入る）
        assertEquals("user001", model.getAttribute("usernameValue"));
        assertEquals("山田太郎", model.getAttribute("displayNameValue"));
        assertEquals("user001@mail.com", model.getAttribute("emailValue"));
        assertEquals("090-1234-5678", model.getAttribute("phoneValue"));
        assertEquals(10, model.getAttribute("departmentIdValue"));
        assertEquals("2", model.getAttribute("roleValue"));

        // リダイレクトではないので flashError は無い
        assertTrue(ra.getFlashAttributes().isEmpty());

        verify(adminUserService).findUserDetail(targetUserId);
        verify(adminUserService).findAllActiveDepartments();
    }

    @Test
    void showUserEditPage_whenFlashValuesExist_doesNotOverwrite() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Integer targetUserId = 1;

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(1); // 自分を編集（isSelf=true）

        Department dept = new Department();
        dept.setId(10);

        User editUser = new User();
        editUser.setId(targetUserId);
        editUser.setUsername("user001");
        editUser.setDisplayName("DB_NAME");
        editUser.setEmail("db@mail.com");
        editUser.setPhone("000");
        editUser.setDepartment(dept);
        editUser.setRole(2);

        when(adminUserService.findUserDetail(targetUserId)).thenReturn(editUser);
        when(adminUserService.findAllActiveDepartments()).thenReturn(List.of());

        // すでにフラッシュ値が model に入っている想定（containsAttribute が true になる）
        model.addAttribute("displayNameValue", "FLASH_NAME");
        model.addAttribute("emailValue", "flash@mail.com");
        model.addAttribute("phoneValue", "090-9999-9999");
        model.addAttribute("departmentIdValue", 20);
        model.addAttribute("roleValue", "1");

        // Act
        String view = adminUserController.showUserEditPage(
                targetUserId, session, model, ra, locale);

        // Assert
        assertEquals("user_edit", view);

        // isSelf は true
        assertEquals(true, model.getAttribute("isSelf"));

        // 既存（フラッシュ）を上書きしない
        assertEquals("FLASH_NAME", model.getAttribute("displayNameValue"));
        assertEquals("flash@mail.com", model.getAttribute("emailValue"));
        assertEquals("090-9999-9999", model.getAttribute("phoneValue"));
        assertEquals(20, model.getAttribute("departmentIdValue"));
        assertEquals("1", model.getAttribute("roleValue"));

        // usernameValue はフラッシュで持たない設計なので、DB値が入るのが自然
        assertEquals("user001", model.getAttribute("usernameValue"));
    }

    @Test
    void showUserEditPage_whenBusinessException_redirectsToList_setsFlashError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Integer targetUserId = 1;

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);

        doThrow(new BusinessException("error.user.notFound"))
                .when(adminUserService).findUserDetail(targetUserId);

        when(messageSource.getMessage("error.user.notFound", null, locale)).thenReturn("NOT_FOUND");

        // Act
        String view = adminUserController.showUserEditPage(
                targetUserId, session, model, ra, locale);

        // Assert
        assertEquals("redirect:/users/list", view);

        Map<String, ?> flash = ra.getFlashAttributes();
        assertEquals("NOT_FOUND", flash.get("flashError"));

        // findUserDetail で落ちるので departments は取りに行かない
        verify(adminUserService).findUserDetail(targetUserId);
        verify(adminUserService, never()).findAllActiveDepartments();
    }

    @Test
    void showUserEditPage_whenUnexpectedException_redirectsToList_setsSystemError() {
        // Arrange
        Locale locale = Locale.JAPAN;
        Integer targetUserId = 1;

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(session.getAttribute("userId")).thenReturn(999);

        doThrow(new RuntimeException("boom"))
                .when(adminUserService).findUserDetail(targetUserId);

        when(messageSource.getMessage("error.system.unexpected", null, locale)).thenReturn("SYSTEM_ERROR");

        // Act
        String view = adminUserController.showUserEditPage(
                targetUserId, session, model, ra, locale);

        // Assert
        assertEquals("redirect:/users/list", view);
        assertEquals("SYSTEM_ERROR", ra.getFlashAttributes().get("flashError"));
    }
}
