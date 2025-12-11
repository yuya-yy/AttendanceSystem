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
}
