package com.example.attendance.config;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.example.attendance.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 認証・認可・削除済み判定（ログインユーザー）のインターセプター
 * preHandle()で判定を行い、問題があればリダイレクトする。
 * 問題なければ Controller へ進む。
 *
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final MessageSource messageSource;
    private final UserRepository userRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 管理者のみ許可するURLパターン（必要に応じて増やす）
    private final List<String> adminOnlyPatterns = List.of(
            "/users/new",
            "/users/*/edit",
            "/users/*/update",
            "/users/*/delete",
            "/reports/**");

    public AuthInterceptor(MessageSource messageSource, UserRepository userRepository) {
        this.messageSource = messageSource;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String path = getPathWithoutContextPath(request);

        // 1) 認証不要（ログイン画面や静的ファイル）は素通り
        if (isPublicPath(path)) {
            return true;
        }

        // 2) ログインチェック
        HttpSession session = request.getSession(false);
        Integer userId = (session == null) ? null : (Integer) session.getAttribute("userId");

        if (userId == null) {
            // 未ログイン → ログイン画面へ
            String msg = getMessage(request.getLocale(), "error.auth.required");
            putFlashErrorAndRedirect(request, response, msg, request.getContextPath() + "/auth/login");
            return false;
        }

        // 2-2) 削除済みユーザーならログアウト扱い
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            session.invalidate();
            String msg = getMessage(request.getLocale(), "error.auth.userDeleted");
            putFlashErrorAndRedirect(request, response, msg, request.getContextPath() + "/auth/login");
            return false;
        }

        // 3) 管理者チェック（管理者専用URLだけ）
        if (isAdminOnlyPath(path)) {
            Integer role = (Integer) session.getAttribute("role"); // 1=admin, 2=user
            if (role == null || role.intValue() != 1) {
                String msg = getMessage(request.getLocale(), "error.auth.forbidden");
                putFlashErrorAndRedirect(request, response, msg, request.getContextPath() + "/attendance");
                return false;
            }
        }

        // OK：Controllerへ進める
        return true;
    }

    /**
     * contextPath を除いたパスを返す（例：/attendance など）
     */
    private String getPathWithoutContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return uri;
        }
        return uri.substring(contextPath.length());
    }

    /**
     * 認証不要URL判定
     */
    private boolean isPublicPath(String path) {
        // ログイン画面は除外（ここを除外しないと無限リダイレクト）
        if (pathMatcher.match("/auth/login", path))
            return true;
        // 例外：エラーページ
        if (pathMatcher.match("/error", path))
            return true;

        // 静的リソース（CSS/JS/画像など）
        if (pathMatcher.match("/css/**", path))
            return true;
        if (pathMatcher.match("/js/**", path))
            return true;
        if (pathMatcher.match("/images/**", path))
            return true;
        if (pathMatcher.match("/webjars/**", path))
            return true;
        if (pathMatcher.match("/favicon.ico", path))
            return true;

        return false;
    }

    /**
     * 管理者専用URL判定
     */
    private boolean isAdminOnlyPath(String path) {
        return adminOnlyPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * messages.properties からメッセージを取得
     */
    private String getMessage(Locale locale, String key) {
        return messageSource.getMessage(key, null, locale);
    }

    /**
     * FlashMap（リダイレクト後に1回だけ表示されるメッセージ）を入れて redirect
     */
    private void putFlashErrorAndRedirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            String redirectUrl) throws IOException {

        FlashMap flashMap = new FlashMap();
        flashMap.put("flashError", message);

        FlashMapManager flashMapManager = RequestContextUtils.getFlashMapManager(request);
        if (flashMapManager != null) {
            flashMapManager.saveOutputFlashMap(flashMap, request, response);
        }

        response.sendRedirect(redirectUrl);
    }
}
