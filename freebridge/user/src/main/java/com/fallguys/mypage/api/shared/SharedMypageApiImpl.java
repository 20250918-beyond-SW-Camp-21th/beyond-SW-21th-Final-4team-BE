package com.fallguys.mypage.api.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SharedMypageApiImpl implements SharedMypageApi {

    @Override
    public void updatePassword(String updatedPassword) {
        // TODO: 향후 외부 서비스(User 모듈)로 비밀번호 변경 요청을 전달하도록 구현.
        // 현재는 FeignClient를 대신하는 포트 역할이므로, 모킹 또는 로깅으로 대체.
        log.info("SharedMypageApi: 외부 User 모듈로 비밀번호 변경(updatePassword) 요청이 전달되었습니다.");
    }
}
