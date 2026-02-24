local failKey = KEYS[1]
local codeKey = KEYS[2]
local maxAttempts = tonumber(ARGV[1])
local providedCode = ARGV[2]
local failTtl = tonumber(ARGV[3])

-- 1. 차단 상태 확인
local failCount = tonumber(redis.call('GET', failKey) or '0')
if failCount >= maxAttempts then
    return -1 -- BLOCKED
end

-- 2. 저장된 코드 확인
local storedCode = redis.call('GET', codeKey)
if not storedCode then
    return -2 -- MISSING
end

-- 3. 코드 비교
if storedCode == providedCode then
    -- 성공: 두 키 삭제
    redis.call('DEL', codeKey, failKey)
    return 1 -- SUCCESS
else
    -- 실패: 카운트 증가
    local newFailCount = redis.call('INCR', failKey)
    if newFailCount == 1 then
        redis.call('EXPIRE', failKey, failTtl)
    end
    
    -- 최대 실패 시 코드 즉시 삭제
    if newFailCount >= maxAttempts then
        redis.call('DEL', codeKey)
        return -3 -- MAX_FAIL_REACHED
    end
    
    return 0 -- MISMATCH
end
