package com.example.attendance.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import com.example.attendance.entity.AttendanceRecord;
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
    public String showUserEditPage(@PathVariable("userId") Integer userId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        Integer sessionUserId = (Integer) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");

        // 未ログイン → ログイン画面へ
        if (sessionUserId == null) {
            String msg = messageSource.getMessage("error.auth.required", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/auth/login";
        }

        // 一般ユーザー → 一覧に戻す
        if (role == null || role != 1) {
            String msg = messageSource.getMessage("error.auth.forbidden", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/list";
        }

        try {
            User editUser = adminUserService.findUserDetail(userId);
            model.addAttribute("editUser", editUser);
            model.addAttribute("departments", adminUserService.findAllActiveDepartments());

            // 「自分自身を編集しているか？」のフラグ（view で使いたければ）
            boolean isSelf = sessionUserId.equals(userId);
            model.addAttribute("isSelf", isSelf);

            // ================================
            // ここから「フォーム値のセット部分」
            // ================================

            // ユーザー名（表示のみ用）
            if (!model.containsAttribute("usernameValue")) {
                model.addAttribute("usernameValue", editUser.getUsername());
            }

            // ★ 氏名：フラッシュ属性があればそれを優先、なければDBの値
            if (!model.containsAttribute("displayNameValue")) {
                model.addAttribute("displayNameValue", editUser.getDisplayName());
            }

            // ★ メールアドレス
            if (!model.containsAttribute("emailValue")) {
                model.addAttribute("emailValue", editUser.getEmail());
            }

            // ★ 電話番号
            if (!model.containsAttribute("phoneValue")) {
                model.addAttribute("phoneValue", editUser.getPhone());
            }

            // ★ 所属部署ID
            if (!model.containsAttribute("departmentIdValue")) {
                Integer deptId = (editUser.getDepartment() != null)
                        ? editUser.getDepartment().getId()
                        : null;
                model.addAttribute("departmentIdValue", deptId);
            }

            // ★ 権限（1 / 2 を String として扱うと Thymeleaf 側が楽）
            if (!model.containsAttribute("roleValue")) {
                String roleStr = (editUser.getRole() != null)
                        ? String.valueOf(editUser.getRole())
                        : null;
                model.addAttribute("roleValue", roleStr);
            }
            return "user_edit";
        } catch (BusinessException e) {
            String msg = messageSource.getMessage(e.getMessageKey(), null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/list";
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.system.unexpected", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/list";
        }
    }

    /**
     * ユーザー更新処理（POST /users/{userId}/update）
     * パスワードが空欄の場合は変更しない。
     */
    @PostMapping("users/{userId}/update")
    public String updateUser(@PathVariable("userId") Integer userId,
            @RequestParam("username") String username,
            @RequestParam("displayName") String displayName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            // ★ ここは一旦 String で受け取る（未選択だと "" が来る）
            @RequestParam(name = "departmentId", required = false) String departmentIdStr,
            @RequestParam(name = "role", required = false) String roleStr,
            @RequestParam(name = "password", required = false) String password,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        Integer sessionUserId = (Integer) session.getAttribute("userId");
        Integer sessionRole = (Integer) session.getAttribute("role");

        // 未ログイン
        if (sessionUserId == null) {
            String msg = messageSource.getMessage("error.auth.required", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/auth/login";
        }

        // 一般ユーザーは禁止
        if (sessionRole == null || sessionRole != 1) {
            String msg = messageSource.getMessage("error.auth.forbidden", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/list";
        }

        try {
            // ★ String → Integer 変換（空欄は null）
            Integer departmentId = null;
            if (departmentIdStr != null && !departmentIdStr.isBlank()) {
                departmentId = Integer.valueOf(departmentIdStr);
            }

            Integer roleParam = null;
            if (roleStr != null && !roleStr.isBlank()) {
                roleParam = Integer.valueOf(roleStr);
            }

            // ★ 管理者が自分自身を一般(2)に落とすのは禁止
            if (sessionUserId.equals(userId) && roleParam != null && roleParam == 2) {
                throw new BusinessException("error.user.role.protected");
            }

            // ★ Service に Integer を渡す（必須チェックは Service 側で）
            adminUserService.updateUser(
                    userId,
                    username,
                    displayName,
                    email,
                    phone,
                    departmentId,
                    roleParam,
                    password // 空欄なら Service 側で「変更なし」
            );

            String msg = messageSource.getMessage("info.user.update.success", null, locale);
            redirectAttributes.addFlashAttribute("flashInfo", msg);

            return "redirect:/users/list";

        } catch (BusinessException e) {
            String msg = messageSource.getMessage(e.getMessageKey(), null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            redirectAttributes.addFlashAttribute("usernameValue", username);
            redirectAttributes.addFlashAttribute("displayNameValue", displayName);
            redirectAttributes.addFlashAttribute("emailValue", email);
            redirectAttributes.addFlashAttribute("phoneValue", phone);
            redirectAttributes.addFlashAttribute("departmentIdValue", departmentIdStr);
            redirectAttributes.addFlashAttribute("roleValue", roleStr);
            return "redirect:/users/" + userId + "/edit";
        } catch (Exception e) {
            e.printStackTrace(); // 開発中はログ見えた方が原因追いやすいです
            String msg = messageSource.getMessage("error.system.unexpected", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/" + userId + "/edit";
        }
    }

    /**
     * 勤怠実績一覧画面（S0107）を表示する（GET /reports/{userId}）
     *
     * 日別の出勤／退勤／勤務時間と、月別の合計勤務時間を表示する画面。
     */
    @GetMapping("/reports/{userId}")
    public String showWorkReportPage(
            @PathVariable("userId") Integer targetUserId, // URLの userId → targetUserId
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale,
            Model model) {
        // 後で userId を使って勤怠実績を取得・集計する

        // ★ 未ログインチェック
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            String message = messageSource.getMessage(
                    "error.auth.required",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/auth/login";
        }

        // ★ 権限チェック（管理者のみ）
        Integer role = (Integer) session.getAttribute("role");
        if (role == null || role != 1) { // 1 = 管理者
            String message = messageSource.getMessage(
                    "error.auth.forbidden",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
            return "redirect:/attendance";
        }

        try {

            // 1) 対象ユーザー情報
            User targetUser = adminUserService.findUserDetail(targetUserId);
            model.addAttribute("targetUser", targetUser);

            // 2) 日別一覧
            List<AttendanceRecord> dailyRecords = adminUserService.getDailyWorkRecords(targetUserId);
            model.addAttribute("dailyRecords", dailyRecords);

            // 3) 月別サマリ一覧
            Map<YearMonth, Long> monthlySummary = adminUserService.getMonthlyWorkSummary(targetUserId);
            model.addAttribute("monthlySummary", monthlySummary);

            return "work_report";

        } catch (BusinessException e) {
            // 業務エラー（ユーザーが存在しないなど）
            String msg = messageSource.getMessage(e.getMessageKey(), null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/list";

        } catch (Exception e) {
            // 想定外エラー
            String msg = messageSource.getMessage("error.system.unexpected", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/users/list";
        }
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
