package com.zj.aiagent.interfaces.common.interceptor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthStrategyFactory {

    private final Map<String, AuthStrategy> strategyMap = new HashMap<>();
    private final JwtAuthStrategy jwtAuthStrategy;
    private final DebugAuthStrategy debugAuthStrategy;

    @PostConstruct
    public void init() {
        strategyMap.put("JWT", jwtAuthStrategy);
        strategyMap.put("DEBUG", debugAuthStrategy);
    }

    public Optional<AuthStrategy> getStrategy(String type) {
        return Optional.ofNullable(strategyMap.get(type));
    }
}
