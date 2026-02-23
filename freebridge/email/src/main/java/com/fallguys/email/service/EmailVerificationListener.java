package com.fallguys.email.service;

/**
 * 이메일 인증이 성공했을 때 호출될 리스너 인터페이스
 * 의존성 역전을 위해 email 모듈에서 정의하고 user 모듈에서 구현함
 */
public interface EmailVerificationListener {
    void onVerificationSuccess(String email);
}
