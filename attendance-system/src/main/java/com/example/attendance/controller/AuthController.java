package com.example.attendance.controller;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.service.AuthService;

import jakarta.servlet.http.HttpSession;

/**
 * 認証（ログイン／ログアウト）を担当する Controller。
 *
 * 役割：
 * - ログイン画面の表示（GET /auth/login）
 * - ログイン処理（POST /auth/login）
 * - ログアウト処理（POST /auth/logout）
 *
 * 業務ロジック（認証チェック）は AuthService に任せ、
 * ここでは「画面遷移」と「セッションへの保存」「フラッシュメッセージ設定」を担当する。
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final MessageSource messageSource;

    public AuthController(AuthService authService, MessageSource messageSource) {
        this.authService = authService;
        this.messageSource = messageSource;
    }

    /**
     * ログイン画面を表示する（GET /auth/login）
     */
    @GetMapping("/login")
    public String showLoginPage() {
        // resources/templates/login.html を表示
        return "login";
    }

    /**
     * ログイン処理を実行する（POST /auth/login）
     *
     * 正常系：
     * - 認証成功 → セッションにユーザー情報を保存 → 勤怠入力画面へリダイレクト
     *
     * 異常系：
     * - BusinessException を受け取ったら、その messageKey を使って
     * messages.properties から日本語メッセージを取得し flashError に詰めて
     * 再度ログイン画面へリダイレクトする。
     * - 想定外の Exception の場合は、共通システムエラー（error.system.unexpected）を表示する。
     */
    @PostMapping("/login")
    public String executeLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        try {
            // 1) 認証処理（成功すれば User が返る）
            User user = authService.authenticate(username, password);

            // 2) セッションに必要な情報を保存
            session.setAttribute("userId", user.getId());
            session.setAttribute("displayName", user.getDisplayName());
            session.setAttribute("role", user.getRole());
            if (user.getDepartment() != null) {
                session.setAttribute("departmentId", user.getDepartment().getId());
            }

            // 3) ログイン成功時は勤怠入力画面（S0102）へ
            return "redirect:/attendance";

        } catch (BusinessException e) {
            // ★ 業務エラー（入力不足・認証失敗など）

            String messageKey = e.getMessageKey();

            String message = messageSource.getMessage(
                    messageKey, // 例：error.auth.password.required など
                    null,
                    locale);

            // エラーメッセージは常に表示
            redirectAttributes.addFlashAttribute("flashError", message);

            // ★★ ここがポイント：パターン別に loginUsername を渡すか分ける ★★
            if ("error.auth.password.required".equals(messageKey)) {
                // パスワードが空欄のときだけ、ユーザー名を保持する
                redirectAttributes.addFlashAttribute("loginUsername", username);
            }
            // それ以外（error.auth.loginFailed 等）は username を渡さないので、
            // 画面側では空欄になる（＝再入力させる）

            return "redirect:/auth/login";

        } catch (Exception e) {
            // 想定外のエラー（プログラムバグ／DB障害など）

            String message = messageSource.getMessage(
                    "error.system.unexpected",
                    null,
                    locale);

            redirectAttributes.addFlashAttribute("flashError", message);

            return "redirect:/auth/login";
        }
    }

    /**
     * ログアウト処理（POST /auth/logout）
     *
     * セッションを破棄して、ログイン画面へ戻す。
     * 成功時には info.auth.loggedOut のメッセージを表示する。
     */
    @GetMapping("/logout")
    public String logout(HttpSession session,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        try {
            // ★ セッションがあれば破棄（ログアウトの本体）
            if (session != null) {
                session.invalidate();
            }

            // ★ ここに「info.auth.loggedOut」を取得するコードは書かない

        } catch (Exception e) {
            // ここは「本当に何か異常が起きたとき」だけシステムエラーを出したい場合
            String message = messageSource.getMessage(
                    "error.system.unexpected",
                    null,
                    locale);
            redirectAttributes.addFlashAttribute("flashError", message);
        }

        // 成功しても失敗しても、とりあえずログイン画面へ戻す
        return "redirect:/auth/login";
    }
}
