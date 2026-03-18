package com.fallguys.common.port;

public interface FileStorage {

    /**
     * @param key 저장 키 (예: user/123/profile.png)
     * @return 저장된 key
     */
    String upload(byte[] bytes, String key, String contentType);

    void deleteByKey(String key);

    String generatePresignedUrl(String key);

    String generatePresignedDownloadUrl(String key, String fileName);
}
