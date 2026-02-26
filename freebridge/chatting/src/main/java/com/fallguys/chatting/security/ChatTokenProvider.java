package com.fallguys.chatting.security;

/**
 * 타 모듈(user) 의존성 분리를 위해 채팅 모듈 내부에 정의한 인터페이스입니다.
 * 실제 구현체는 app-main 모듈이나 JWT 제공 모듈에서 주입(DI)합니다.
 */
public interface ChatTokenProvider {
    /**
     * JWT 토큰을 검증하고 유효한 경우 유저 식별자 ID를 추출하여 반환합니다.
     * 
     * @param token Bearer 식별자가 제거된 순수 토큰 문자열
     * @return 유저 ID (예: e1, f1 등)
     * @throws IllegalArgumentException 토큰이 유효하지 않을 경우 발생
     */
    String getUserIdFromToken(String token);
}
