package com.fallguys.user.api.shared;

import com.fallguys.user.api.shared.response.ExternalUserResponse;

/**
 * 타 모듈에서 User 정보를 조회하기 위해 사용할 인터페이스입니다.
 * 이 인터페이스를 통해 user 모듈 내부의 UserService와 강한 결합 없이 안전한 통신이 가능합니다.
 */
public interface ExternalUserApi {

    /**
     * 사용자 ID로 유저 정보를 조회합니다.
     * 
     * @param userId 조회할 사용자의 고유 ID
     * @return 유저 정보 DTO (존재하지 않으면 예외 발생)
     */
    ExternalUserResponse getUserById(Long userId);

    /**
     * 이메일로 유저 정보를 조회합니다.
     * 
     * @param email 조회할 사용자의 이메일 주소
     * @return 유저 정보 DTO (존재하지 않으면 예외 발생)
     */
    ExternalUserResponse getUserByEmail(String email);

    /**
     * 사용자 ID 존재 여부를 확인합니다.
     * 
     * @param userId 확인할 사용자 고유 ID
     * @return 존재할 경우 true, 그렇지 않으면 false
     */
    boolean existsById(Long userId);

    /**
     * 사용자 ID로 비밀번호를 변경합니다.
     * 타 모듈(예: 관리자 기능 등)에서 비밀번호 강제 변경이 필요할 때 사용합니다.
     * 
     * @param userId 변경할 대상 사용자의 고유 ID
     * 
     * @param newPassword 변경할 새로운 비밀번호 (암호화되어야 함)
     */
    void updatePassword(Long userId, String newPassword);

    /**
     * 사용자 ID로 이메일 알림 수신 동의 설정을 변경합니다.
     * 
     * @param userId 변경할 대상 사용자의 고유 ID
     * 
     * @param emailEnabled 이메일 알림 수신 여부
     */
    void updateEmailNotificationSetting(Long userId, boolean emailEnabled);
}
