package com.zj.aiagent.interfaces.common.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtAuthStrategy implements AuthStrategy {

    @Value("${jwt.secret:defaultSecret}")
    private String jwtSecret;

    @Override
    public boolean authenticate(String token, Object... extraArgs) {
        try {
            // Basic JWT verification logic
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims != null;
        } catch (Exception e) {
            log.warn("JWT Verification failed: {}", e.getMessage());
            return false;
        }
    }
}
