package com.zj.aiagent.interfaces.common.interceptor;

import com.zj.aiagent.config.AuthDebugProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebugAuthStrategy implements AuthStrategy {

    private final AuthDebugProperties debugProperties;

    @Override
    public boolean authenticate(String token, Object... extraArgs) {
        if (!debugProperties.isEnabled()) {
            return false;
        }
        // Token here is actually the userId passed from header
        if (token == null || token.isBlank()) {
            return false;
        }

        // In a real implementation, you might want to verify if the userId exists in
        // DB.
        // For now, as per request "direct use", we consider it valid if it's provided.
        try {
            Long.parseLong(token); // Ensure it's a valid ID format
            return true;
        } catch (NumberFormatException e) {
            log.warn("Invalid User ID format in Debug Header: {}", token);
            return false;
        }
    }
}
