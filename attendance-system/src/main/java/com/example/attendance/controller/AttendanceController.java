package com.example.attendance.controller;

import java.util.Locale;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.entity.User;

import jakarta.servlet.http.HttpSession;

/**
 * 勤怠まわり（出勤・退勤、勤務場所、連絡先、勤怠状況一覧）を扱う Controller の土台。
 *
 * Laravel でいうと、AttendanceController 的なクラスで、
 * 各画面の「表示用アクション」だけ先に作っているイメージです。
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final MessageSource messageSource;

    public AttendanceController(AttendanceService attendanceService,
            MessageSource messageSource) {
        this.attendanceService = attendanceService;
        this.messageSource = messageSource;
    }

    /**
     * 勤怠入力画面を表示する（GET /attendance）
     * - 出勤中かどうか
     * - 現在の勤務場所
     * - 直近30日分の履歴
     * を model に詰めて attendance.html を表示。
     */
    @GetMapping
    public String showAttendancePage(HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        Integer role = (Integer) session.getAttribute("role");
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            String msg = messageSource.getMessage("error.auth.required", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/auth/login";
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Tokyo"));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M月 d日 (E)", Locale.JAPANESE);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.JAPANESE);

        String todayDateText = now.format(dateFormatter); // 例: 12月 10日 (水)
        String nowTimeText = now.format(timeFormatter); // 例: 14:35

        model.addAttribute("todayDateText", todayDateText);
        model.addAttribute("nowTimeText", nowTimeText);

        User loginUser = attendanceService.findUserById(userId);
        String displayName = loginUser.getDisplayName();
        String departmentName = loginUser.getDepartment().getDepartmentName();

        String roleLabel = (role != null && role == 1) ? "管理者" : "一般";

        // 2) 未退勤レコード（あれば「今勤務中」）
        AttendanceRecord latest = attendanceService.findLatestUnfinished(userId);

        // 3) 直近30日分の履歴（勤務状態ラベル判定にも使う）
        List<AttendanceRecord> recentRecords = attendanceService.getRecentRecords(userId);

        // 4) システム内部用フラグ（勤務中 true / 勤務外 false）
        boolean workingNow = (latest != null);

        // 5) 画面表示用ラベル（未出勤／出勤中／退勤済み）
        String workStatusLabel;
        if (latest != null) {
            // 未退勤レコードあり → 出勤中
            workStatusLabel = "出勤中";
        } else if (recentRecords.isEmpty()) {
            // 履歴が1件もない → 今日は一度も出勤していない → 未出勤
            workStatusLabel = "未出勤";
        } else {
            // 履歴はあるが未退勤レコードはない → 今日は出勤済み＆退勤済み
            workStatusLabel = "退勤済み";
        }

        // 6) 現在の勤務場所名（S0103の設定値）
        String currentWorkLocationName = attendanceService.getCurrentWorkLocationName(userId);

        // 7) 画面に渡す値をセット
        model.addAttribute("displayName", displayName);
        model.addAttribute("departmentName", departmentName);
        model.addAttribute("roleLabel", roleLabel);
        model.addAttribute("workingNow", workingNow); // true/false（他画面ロジック用）
        model.addAttribute("workStatusLabel", workStatusLabel); // 「未出勤／出勤中／退勤済み」
        model.addAttribute("latestRecord", latest); // 必要ならテンプレで使える
        model.addAttribute("currentWorkLocationName", currentWorkLocationName);
        model.addAttribute("recentRecords", recentRecords); // テーブル表示用

        return "attendance";
    }

    /**
     * 出勤ボタン（POST /attendance/clock-in）
     */
    @PostMapping("/clock-in")
    public String clockIn(HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            String msg = messageSource.getMessage("error.auth.required", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/auth/login";
        }

        try {
            attendanceService.recordClockIn(userId);
            String msg = messageSource.getMessage("info.attendance.clockIn.saved", null, locale);
            redirectAttributes.addFlashAttribute("flashInfo", msg);
        } catch (BusinessException e) {
            String msg = messageSource.getMessage(e.getMessageKey(), null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.system.unexpected", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
        }

        return "redirect:/attendance";
    }

    /**
     * 退勤ボタン（POST /attendance/clock-out）
     */
    @PostMapping("/clock-out")
    public String clockOut(HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            String msg = messageSource.getMessage("error.auth.required", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
            return "redirect:/auth/login";
        }

        try {
            attendanceService.recordClockOut(userId);
            String msg = messageSource.getMessage("info.attendance.clockOut.saved", null, locale);
            redirectAttributes.addFlashAttribute("flashInfo", msg);
        } catch (BusinessException e) {
            String msg = messageSource.getMessage(e.getMessageKey(), null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.system.unexpected", null, locale);
            redirectAttributes.addFlashAttribute("flashError", msg);
        }

        return "redirect:/attendance";
    }

    /**
     * 勤務場所登録画面を表示する（GET /attendance/work-location）
     */
    @GetMapping("/work-location")
    public String showWorkLocationPage() {
        // resources/templates/workplace.html
        return "workplace";
    }

    /**
     * 連絡先登録画面を表示する（GET /attendance/contact）
     */
    @GetMapping("/contact")
    public String showContactPage() {
        // resources/templates/contact.html
        return "contact";
    }

    /**
     * 勤怠状況一覧画面を表示する（GET /attendance/status）
     * 同じ部署メンバーの現在の勤務状況を一覧表示する画面。
     */
    @GetMapping("/status")
    public String showDepartmentStatusPage() {
        // resources/templates/status_list.html
        return "status_list";
    }
}
