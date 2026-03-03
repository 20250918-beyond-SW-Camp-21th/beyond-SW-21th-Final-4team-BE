package com.fallguys.mypage.api.shared;

public interface SharedMypageApi {

    /**
     * User 도메인에 비밀번호 변경을 요청하는 포트 인터페이스입니다.
     * (추후 FeignClient 등으로 치환될 예정)
     *
     * @param updatedPassword 변경할 새 비밀번호
     */
    void updatePassword(String updatedPassword);
}
