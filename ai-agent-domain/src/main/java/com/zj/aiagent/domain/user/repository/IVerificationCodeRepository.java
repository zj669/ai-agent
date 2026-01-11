package com.zj.aiagent.domain.user.repository;

import com.zj.aiagent.domain.user.valobj.Email;

public interface IVerificationCodeRepository {
    void save(Email email, String code, long expirySeconds);

    String get(Email email);

    void remove(Email email);
}
