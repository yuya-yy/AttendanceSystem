package com.example.attendance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * インターセプターをどのURLに適用するかを設定するクラス
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 基本は全てに適用し、除外は Interceptor 側で判断（isPublicPath）に寄せる
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**");
    }
}
