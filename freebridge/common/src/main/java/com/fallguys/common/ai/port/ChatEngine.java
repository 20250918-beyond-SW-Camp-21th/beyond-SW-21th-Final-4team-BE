package com.fallguys.common.ai.port;

import java.util.Map;

public interface ChatEngine {
    String askChatBot(String question, Map<String, Object> context);
}