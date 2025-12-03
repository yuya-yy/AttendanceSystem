package com.example.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ユーザー情報一覧・ユーザー管理・勤怠実績画面を扱う Controller の土台。
 *
 * Laravel でいうと、UserController + Admin用の機能をまとめたクラスで、
 * index / create / edit / show 的な画面表示を担当するイメージです。
 */
@Controller
@RequestMapping
public class AdminUserController {

    /**
     * ユーザー情報一覧画面を表示する（GET /users/list）
     * 全ユーザーの一覧を表示する画面。
     */
    @GetMapping("/users/list")
    public String showUserListPage() {
        // resources/templates/user_list.html
        return "user_list";
    }

    /**
     * 新規ユーザー登録画面を表示する（GET /users/new）
     */
    @GetMapping("/users/new")
    public String showUserNewPage() {
        // resources/templates/user_new.html
        return "user_new";
    }

    /**
     * ユーザー編集画面を表示する（GET /users/{userId}/edit）
     *
     * 将来は userId を使ってユーザー情報を取得し、
     * Model に詰めてから user_edit.html を表示する。
     */
    @GetMapping("/users/edit")
    // @GetMapping("/users/{userId}/edit")
    public String showUserEditPage(/* @PathVariable("userId") Integer userId */) {

        // 後で userId を使ってユーザー情報を Service から取得する
        // 今はテンプレートだけ表示

        return "user_edit";
    }

    /**
     * 勤怠実績一覧画面（S0107）を表示する（GET /reports/{userId}）
     *
     * 日別の出勤／退勤／勤務時間と、月別の合計勤務時間を表示する画面。
     */
    @GetMapping("/reports")
    // @GetMapping("/reports/{userId}")
    public String showWorkReportPage(/* @PathVariable("userId") Integer userId */) {

        // 後で userId を使って勤怠実績を取得・集計する

        return "work_report";
    }
}
