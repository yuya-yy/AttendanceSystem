package com.example.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 認証（ログイン／ログアウト）を担当する Controller の土台クラス。
 * 今はログイン画面を表示するだけの最低限の実装。
 *
 * Laravel でいうと、Auth系のControllerで
 * 「/login を表示するアクション」に相当します。
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    /**
     * ログイン画面を表示する（GET /auth/login）
     * 将来はここにフラッシュメッセージなどを渡すようにする。
     */
    @GetMapping("/login")
    public String showLoginPage() {
        // resources/templates/login.html を表示
        return "login";
    }
}
