package com.example.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * パスワードをハッシュ化／照合するための設定クラス。
 * アプリ全体で PasswordEncoder を1個だけ共有する。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt を使う PasswordEncoder を Bean 登録する。
     * 他のクラスから @Autowired / コンストラクタ経由で使えるようになる。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
