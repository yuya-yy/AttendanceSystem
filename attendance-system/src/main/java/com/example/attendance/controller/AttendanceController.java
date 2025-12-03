package com.example.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 勤怠まわり（出勤・退勤、勤務場所、連絡先、勤怠状況一覧）を扱う Controller の土台。
 *
 * Laravel でいうと、AttendanceController 的なクラスで、
 * 各画面の「表示用アクション」だけ先に作っているイメージです。
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    /**
     * 勤怠入力画面を表示する（GET /attendance）
     * 将来はここで当日の状態や直近30日分の勤怠を Model に詰める。
     */
    @GetMapping
    public String showAttendancePage() {
        // resources/templates/attendance.html
        return "attendance";
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
