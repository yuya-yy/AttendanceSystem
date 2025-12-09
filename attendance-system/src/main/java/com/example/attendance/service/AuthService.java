package com.example.attendance.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;

/**
 * 認証（ログイン）処理を担当する Service。
 *
 * - 入力チェック
 * - ユーザー取得
 * - パスワード照合（ハッシュ）
 * を行い、成功したら User を返します。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // パスワードハッシュ用

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * ログイン認証を行う。
     *
     * @param username    入力されたユーザー名
     * @param rawPassword 入力されたパスワード（平文）
     * @return 認証に成功した User エンティティ
     * @throws BusinessException 認証に失敗した場合（メッセージキー付き）
     */
    public User authenticate(String username, String rawPassword) {

        // 1) 入力チェック（空欄チェック）
        if (username == null || username.isBlank()) {
            throw new BusinessException("error.auth.username.required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException("error.auth.password.required");
        }

        // 2) ユーザー名からユーザーを検索
        // 見つからない場合も「ユーザー名またはパスワードが正しくありません」にまとめる
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BusinessException("error.auth.loginFailed"));

        // 3) パスワード照合（ハッシュ比較）
        if (!matchesPassword(rawPassword, user.getPasswordHash())) {
            // パスワード不一致 → loginFailed にまとめる
            throw new BusinessException("error.auth.loginFailed");
        }

        // 4) 認証成功
        return user;
    }

    /**
     * パスワードの照合処理。
     * DB 側には BCrypt でハッシュ化された文字列が入っている前提。
     */
    private boolean matchesPassword(String rawPassword, String passwordHash) {

        // DB 側が null/空なら一致しようがないので false
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        // BCrypt 方式で「一致しているかだけ」チェックする
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
