package com.example.attendance.common;

/**
 * 業務エラー（ビジネスロジックのエラー）を表す例外クラス。
 *
 * 画面に表示する「日本語メッセージ」そのものは持たず、
 * messages.properties の「メッセージキー」だけを保持します。
 */
public class BusinessException extends RuntimeException {

    /** messages.properties に書くメッセージキー（例：error.auth.loginFailed） */
    private final String messageKey;

    /**
     * メッセージキーを受け取るコンストラクタ。
     *
     * @param messageKey messages.properties のキー
     */
    public BusinessException(String messageKey) {
        // 親クラス（RuntimeException）にもとりあえずキーを渡しておく
        super(messageKey);
        this.messageKey = messageKey;
    }

    /**
     * Controller 側で MessageSource に渡すために使う。
     *
     * @return messages.properties のキー
     */
    public String getMessageKey() {
        return messageKey;
    }
}
