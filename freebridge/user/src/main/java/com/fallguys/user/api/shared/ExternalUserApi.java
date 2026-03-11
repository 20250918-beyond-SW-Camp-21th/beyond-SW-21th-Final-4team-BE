package com.fallguys.user.api.shared;

import com.fallguys.user.api.shared.response.ExternalUserMyInfoResponse;
import com.fallguys.user.api.shared.response.ExternalUserResponse;

/**
 * 외 모듈에서 User 정보를 조회하기 위해 사용하는 인터페이스입니다.
 * 해당 인터페이스를 통해 user 모듈 내부 UserService와 강한 결합 없이 외부와 통신할 수 있습니다.
 */
public interface ExternalUserApi {

    /**
     * 사용자 ID로 회원 정보를 조회합니다.
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 회원 정보 DTO (존재하지 않으면 예외 발생)
     */
    ExternalUserResponse getUserById(Long userId);

    /**
     * 이메일로 회원 정보를 조회합니다.
     *
     * @param email 조회할 사용자의 이메일 주소
     * @return 회원 정보 DTO (존재하지 않으면 예외 발생)
     */
    ExternalUserResponse getUserByEmail(String email);

    /**
     * 마이페이지용 사용자 계정 정보를 조회합니다. (role 제외)
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 사용자 계정 정보 DTO
     */
    ExternalUserMyInfoResponse getMyInfo(Long userId);

    /**
     * 사용자 ID 존재 여부를 확인합니다.
     *
     * @param userId 확인할 사용자 고유 ID
     * @return 존재할 경우 true, 그렇지 않으면 false
     */
    boolean existsById(Long userId);

    /**
     * 사용자 ID로 비밀번호를 변경합니다.
     *
     * @param userId 변경할 대상 사용자의 고유 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 변경할 새로운 비밀번호 (평문 비밀번호, 내부에서 인코딩됨)
     */
    void updatePassword(Long userId, String currentPassword, String newPassword);

    /**
     * 사용자 ID로 이메일 알림 수신 설정을 변경합니다.
     *
     * @param userId 변경할 대상 사용자의 고유 ID
     * @param emailEnabled 이메일 알림 수신 여부
     */
    void updateEmailNotificationSetting(Long userId, boolean emailEnabled);

    /**
     * 사용자 이름을 변경합니다.
     *
     * @param userId 변경할 대상 사용자의 고유 ID
     * @param name 변경할 이름
     */
    void updateName(Long userId, String name);

    /**
     * 사용자 연락처를 변경합니다.
     *
     * @param userId 변경할 사용자 고유 ID
     * @param phone 변경할 연락처
     */
    void updatePhone(Long userId, String phone);
}
