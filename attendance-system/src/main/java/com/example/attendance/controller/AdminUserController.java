package com.example.attendance.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.service.AdminUserService;

import jakarta.servlet.http.HttpSession;

/**
 * ユーザー情報一覧・ユーザー管理・勤怠実績画面を扱う Controller。
 *
 */
@Controller
@RequestMapping
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final MessageSource messageSource;

    public AdminUserController(AdminUserService adminUserService,
            MessageSource messageSource) {
        this.adminUserService = adminUserService;
        this.messageSource = messageSource;
    }

    /**
     * ユーザー情報一覧画面を表示する（GET /users/list）
     * 全ユーザーの一覧を表示する画面。
     *
     */
    @GetMapping("/users/list")
    public String showUserListPage(HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale,
            Model model) {

        // ★ 未ログインチェック
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            String message = messageSource.getMessage(
                    "error.auth.forbidden",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/auth/login";
        }

        // Serviceから取得
        List<User> users = adminUserService.findAllUsers();

        // Model に詰める
        model.addAttribute("users", users);

        // resources/templates/user_list.html
        return "user_list";
    }

    /**
     * 新規ユーザー登録画面を表示する（GET /users/new）
     */
    @GetMapping("/users/new")
    public String showUserNewPage(HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale,
            Model model) {

        // ★ 未ログインチェック
        Integer userId = (Integer) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");
        if (userId == null) {
            String message = messageSource.getMessage(
                    "error.auth.required",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/auth/login";
        }

        // ★ 権限チェック（管理者のみ）
        if (role == null || role != 1) { // 1 = 管理者
            String message = messageSource.getMessage(
                    "error.auth.forbidden",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/attendance";
        }

        // TODO: 部署一覧・勤務場所一覧を Service から取得して model に詰める
        model.addAttribute("departments", adminUserService.getActiveDepartments());
        model.addAttribute("workLocations", adminUserService.getActiveWorkLocations());

        // フラッシュ属性から前回入力値を拾いたい場合はここで model に移す

        // resources/templates/user_new.html
        return "user_new";
    }

    /**
     * 新規ユーザー登録を実行する（POST /users/new）
     */
    @PostMapping("/users/new")
    public String registerUser(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "displayName", required = false) String displayName,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "role", required = false) String roleValue,
            @RequestParam(name = "departmentId", required = false) String departmentIdValue,
            @RequestParam(name = "defaultWorkLocationId", required = false) String defaultWorkLocationIdValue,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        // ===== 1) 未ログイン・権限チェック =====
        Integer sessionUserId = (Integer) session.getAttribute("userId");
        Integer sessionRole = (Integer) session.getAttribute("role");
        if (sessionUserId == null) {
            String message = messageSource.getMessage(
                    "error.auth.required",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/auth/login";
        }
        if (sessionRole == null || sessionRole != 1) {
            String message = messageSource.getMessage(
                    "error.auth.forbidden",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/attendance";
        }

        // ===== 2) String → Integer 変換（空や変な文字列は null にする） =====
        Integer role = parseIntegerOrNull(roleValue);
        Integer departmentId = parseIntegerOrNull(departmentIdValue);
        Integer defaultWorkLocationId = parseIntegerOrNull(defaultWorkLocationIdValue);

        try {
            // ===== 3) Service に登録処理を委譲 =====
            adminUserService.registerUser(
                    username,
                    displayName,
                    password,
                    email,
                    phone,
                    role,
                    departmentId,
                    defaultWorkLocationId);

            // 成功時：一覧画面へ遷移（メッセージはお好みで）
            String success = messageSource.getMessage("info.user.register.success", null, locale);
            redirectAttributes.addFlashAttribute("flashInfo", success);
            return "redirect:/users/list";

        } catch (BusinessException e) {
            // ===== 4) 業務エラー（入力ミスなど）の場合 =====
            String message = messageSource.getMessage(
                    e.getMessageKey(),
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);

            // 入力内容を保持しておく（画面に戻したときに再表示したい値）
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("displayName", displayName);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            redirectAttributes.addFlashAttribute("roleValue", roleValue);
            redirectAttributes.addFlashAttribute("departmentIdValue", departmentIdValue);
            redirectAttributes.addFlashAttribute("defaultWorkLocationIdValue", defaultWorkLocationIdValue);

            return "redirect:/users/new";

        } catch (Exception e) {
            // ===== 5) 想定外のエラーの場合 =====
            String message = messageSource.getMessage(
                    "error.system.unexpected",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/users/new";
        }

    }

    /**
     * 文字列を Integer に変換するヘルパーメソッド。
     *
     * - null や空文字（""）なら null を返す
     * - 数字以外が混ざっている場合も null を返す
     *
     * Service 側では「null なら未選択」として扱い、
     * validation.department.required などの必須チェックでエラーにする。
     */
    private Integer parseIntegerOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            // 変な文字列が来た場合も「未選択」として扱う
            return null;
        }
    }

    /**
     * ユーザー編集画面を表示する（GET /users/edit）
     *
     * 将来は userId を使ってユーザー情報を取得し、
     * Model に詰めてから user_edit.html を表示する。
     */
    @GetMapping("/users/{userId}/edit")
    public String showUserEditPage(@PathVariable("userId") Integer userId) {

        // 後で userId を使ってユーザー情報を Service から取得する
        // 今はテンプレートだけ表示

        return "user_edit";
    }

    /**
     * 勤怠実績一覧画面（S0107）を表示する（GET /reports）
     *
     * 日別の出勤／退勤／勤務時間と、月別の合計勤務時間を表示する画面。
     */
    @GetMapping("/reports/{userId}")
    public String showWorkReportPage(@PathVariable("userId") Integer userId) {
        // 後で userId を使って勤怠実績を取得・集計する
        return "work_report";
    }

    /**
     * ユーザー削除機能、画面は無し、仮置き
     *
     */
    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable("userId") Integer userId) {
        return "user_delete";
    }
}
