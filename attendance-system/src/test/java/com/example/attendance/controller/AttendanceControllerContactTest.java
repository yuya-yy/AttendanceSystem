package com.example.attendance.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.attendance.common.BusinessException;
import com.example.attendance.config.AuthInterceptor;
import com.example.attendance.config.WebMvcConfig;
import com.example.attendance.entity.User;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.UserSettingService;

@WebMvcTest(controllers = AttendanceController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                WebMvcConfig.class, AuthInterceptor.class }))
class AttendanceControllerContactTest {

        @Autowired
        private MockMvc mockMvc;

        // Controllerが依存しているものをモック化（本物を使わない）
        @MockitoBean
        private UserSettingService userSettingService;

        // ★ AttendanceController が AttendanceService を持っている可能性が高いので追加
        @MockitoBean
        private AttendanceService attendanceService;

        @MockitoBean
        private MessageSource messageSource;

        @Test
        @DisplayName("GET /attendance/contact 成功：contact画面を表示し、contactInfoをmodelに詰める")
        void showContactPage_success() throws Exception {
                Integer userId = 1;

                User user = new User();
                user.setEmail("a@mail.com");
                user.setPhone("09012345678");

                when(userSettingService.getContactInfo(userId)).thenReturn(user);

                mockMvc.perform(get("/attendance/contact")
                                .sessionAttr("userId", userId)
                                .locale(Locale.JAPANESE))
                                .andExpect(status().isOk())
                                .andExpect(view().name("contact"))
                                .andExpect(model().attributeExists("contactInfo"));

                verify(userSettingService).getContactInfo(userId);
        }

        @Test
        @DisplayName("GET /attendance/contact 業務エラー：/attendanceへリダイレクト + flashError")
        void showContactPage_businessError_redirectAttendance() throws Exception {
                Integer userId = 1;

                when(userSettingService.getContactInfo(userId))
                                .thenThrow(new BusinessException("validation.phone.numeric"));

                when(messageSource.getMessage(eq("validation.phone.numeric"), isNull(), any(Locale.class)))
                                .thenReturn("電話番号は数字のみで入力してください。");

                mockMvc.perform(get("/attendance/contact")
                                .sessionAttr("userId", userId)
                                .locale(Locale.JAPANESE))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/attendance"))
                                .andExpect(flash().attribute("flashError", "電話番号は数字のみで入力してください。"));
        }

        @Test
        @DisplayName("GET /attendance/contact 想定外エラー：/attendanceへリダイレクト + flashError")
        void showContactPage_unexpectedError_redirectAttendance() throws Exception {
                Integer userId = 1;

                when(userSettingService.getContactInfo(userId))
                                .thenThrow(new RuntimeException("boom"));

                when(messageSource.getMessage(eq("error.system.unexpected"), isNull(), any(Locale.class)))
                                .thenReturn("予期せぬエラーが発生しました。");

                mockMvc.perform(get("/attendance/contact")
                                .sessionAttr("userId", userId)
                                .locale(Locale.JAPANESE))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/attendance"))
                                .andExpect(flash().attribute("flashError", "予期せぬエラーが発生しました。"));
        }

        @Test
        @DisplayName("POST /attendance/contact 成功：/attendance/contactへリダイレクト + flashInfo")
        void updateContact_success_redirectContact() throws Exception {
                Integer userId = 1;

                // voidメソッドなので doNothing() は不要（何もしないのがデフォルト）
                when(messageSource.getMessage(eq("info.contact.saved"), isNull(), any(Locale.class)))
                                .thenReturn("連絡先を保存しました。");

                mockMvc.perform(post("/attendance/contact")
                                .sessionAttr("userId", userId)
                                .param("email", "test@mail.com")
                                .param("phone", "090-1111-2222")
                                .locale(Locale.JAPANESE))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/attendance/contact"))
                                .andExpect(flash().attribute("flashInfo", "連絡先を保存しました。"));

                verify(userSettingService).updateContactInfo(userId, "test@mail.com", "090-1111-2222");
        }

        @Test
        @DisplayName("POST /attendance/contact 業務エラー：/attendance/contactへリダイレクト + flashError + 入力保持")
        void updateContact_businessError_redirectContact_withInput() throws Exception {
                Integer userId = 1;

                doThrow(new BusinessException("validation.email.format"))
                                .when(userSettingService).updateContactInfo(userId, "bad-mail", "09012345678");

                when(messageSource.getMessage(eq("validation.email.format"), isNull(), any(Locale.class)))
                                .thenReturn("メールアドレスの形式が正しくありません。");

                mockMvc.perform(post("/attendance/contact")
                                .sessionAttr("userId", userId)
                                .param("email", "bad-mail")
                                .param("phone", "09012345678")
                                .locale(Locale.JAPANESE))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/attendance/contact"))
                                .andExpect(flash().attribute("flashError", "メールアドレスの形式が正しくありません。"))
                                .andExpect(flash().attribute("formEmail", "bad-mail"))
                                .andExpect(flash().attribute("formPhone", "09012345678"));
        }

        @Test
        @DisplayName("POST /attendance/contact 想定外エラー：/attendance/contactへリダイレクト + flashError + 入力保持")
        void updateContact_unexpectedError_redirectContact_withInput() throws Exception {
                Integer userId = 1;

                doThrow(new RuntimeException("boom"))
                                .when(userSettingService).updateContactInfo(userId, "test@mail.com", "09012345678");

                when(messageSource.getMessage(eq("error.system.unexpected"), isNull(), any(Locale.class)))
                                .thenReturn("予期せぬエラーが発生しました。");

                mockMvc.perform(post("/attendance/contact")
                                .sessionAttr("userId", userId)
                                .param("email", "test@mail.com")
                                .param("phone", "09012345678")
                                .locale(Locale.JAPANESE))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/attendance/contact"))
                                .andExpect(flash().attribute("flashError", "予期せぬエラーが発生しました。"))
                                .andExpect(flash().attribute("formEmail", "test@mail.com"))
                                .andExpect(flash().attribute("formPhone", "09012345678"));
        }
}
