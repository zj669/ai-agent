package com.zj.aiagent.domain.auth.service;

import com.zj.aiagent.domain.user.entity.User;

public interface ITokenService {
    String createToken(User user);

    void invalidateToken(String token);

    boolean validateToken(String token);

    Long getUserIdFromToken(String token);
}
