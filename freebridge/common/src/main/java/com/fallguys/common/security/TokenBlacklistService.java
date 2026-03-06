package com.fallguys.common.security;

public interface TokenBlacklistService {
    /**
     * Checks if the given token is present in the blacklist.
     * 
     * @param token The JWT access token string
     * @return true if blacklisted (logged out), false otherwise
     */
    boolean isBlacklisted(String token);
}
