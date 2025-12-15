package com.example.attendance.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance.common.BusinessException;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkLocation;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkLocationRepository;

/**
 * ユーザー本人の「勤務場所設定」「連絡先設定」などを扱う Service。
 *
 * 今回はまず「勤務場所登録（デフォルト勤務場所の設定）」だけ実装します。
 */
@Service
public class UserSettingService {

    private final UserRepository userRepository;
    private final WorkLocationRepository workLocationRepository;

    public UserSettingService(UserRepository userRepository,
            WorkLocationRepository workLocationRepository) {
        this.userRepository = userRepository;
        this.workLocationRepository = workLocationRepository;
    }

    /**
     * プルダウン／ラジオボタン用に、
     * 有効な勤務場所マスタ一覧を取得する。
     */
    @Transactional(readOnly = true)
    public List<WorkLocation> getActiveWorkLocations() {
        return workLocationRepository.findAllActive();
    }

    /**
     * 現在のデフォルト勤務場所IDを取得する。
     * （ユーザーに既に設定されていればその ID、なければ null）
     */
    @Transactional(readOnly = true)
    public Integer getCurrentWorkLocationId(Integer userId) {

        return userRepository.findById(userId)
                .filter(User::isActive) // 論理削除されていないユーザーだけ
                .map(user -> {
                    WorkLocation loc = user.getDefaultWorkLocation();
                    return (loc != null) ? loc.getId() : null;
                })
                .orElse(null);
    }

    /**
     * デフォルト勤務場所を更新する。
     *
     * @param userId         セッション中のユーザーID
     * @param workLocationId 選択された勤務場所ID（null の場合はエラーにする）
     */
    @Transactional
    public void updateWorkLocation(Integer userId, Integer workLocationId) {

        // 1) ユーザー存在チェック
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("error.user.notFound"));

        // 論理削除されていたらエラーにする（message key はお好みで）
        if (!user.isActive()) {
            throw new BusinessException("error.auth.userDeleted");
        }

        // 2) 勤務場所IDは必須（今回の仕様では「必ずどれか選ぶ」想定）
        if (workLocationId == null) {
            throw new BusinessException("error.workLocation.required");
        }

        // 3) 勤務場所マスタの取得
        WorkLocation workLocation = workLocationRepository.findById(workLocationId)
                .orElseThrow(() -> new BusinessException("error.workLocation.notFound"));

        if (!workLocation.isActive()) {
            throw new BusinessException("error.workLocation.notFound");
        }

        // 4) ユーザーに勤務場所を紐づけて保存
        user.setDefaultWorkLocation(workLocation);
        user.setUpdatedAt(OffsetDateTime.now());

        userRepository.save(user);
    }
    
    /**
     * 連絡先情報を取得する。
     * 連絡先登録画面（S0104）に表示するために、ログインユーザーのメールアドレス・電話番号を取得する。
     *
     * @param userId ログインユーザーID
     * @return 連絡先情報（メールアドレス、電話番号）を含むユーザー情報。見つからない場合は null
     */
    @Transactional(readOnly = true)
    public User getContactInfo(Integer userId) {
        return userRepository.findById(userId)
                .filter(User::isActive) // 論理削除されていないユーザーだけ
                .orElse(null);
    }

    /**
     * 連絡先情報を更新する。
     * 連絡先登録画面（S0104）から送信されたメールアドレス・電話番号で、ログインユーザーの連絡先情報を更新する。
     *
     * @param userId      ログインユーザーID
     * @param email       メールアドレス
     * @param phoneNumber 電話番号
     */
    @Transactional
    public void updateContactInfo(Integer userId, String email, String phoneNumber) {

        String trimmedEmail = (email != null) ? email.trim() : null;
        String trimmedPhone = (phoneNumber != null) ? phoneNumber.trim() : null;
        // 1) ユーザー存在チェック
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("error.user.notFound"));

        // 論理削除されていたらエラーにする
        if (!user.isActive()) {
            throw new BusinessException("error.auth.userDeleted");
        }

        // 2-3) メールアドレス形式（任意入力：入っている場合だけチェック）
        if (trimmedEmail != null && !trimmedEmail.isBlank()) {
            String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (!trimmedEmail.matches(emailPattern)) {
                throw new BusinessException("validation.email.format");
            }
        }

        // 2-4) 電話番号形式（任意入力：入っている場合だけチェック）
        if (trimmedPhone != null && !trimmedPhone.isBlank()) {
            if (!trimmedPhone.matches("^[0-9]+$")) {
                throw new BusinessException("validation.phone.numeric");
            }
        }

        // 2) 連絡先情報の更新
        user.setEmail(email);
        user.setPhone(phoneNumber);
        user.setUpdatedAt(OffsetDateTime.now());

        userRepository.save(user);
    }

    
}
