package com.zj.aiagent.domain.user.repository;

import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.valobj.Email;

public interface IUserRepository {
    User save(User user);

    User findById(Long id);

    User findByEmail(Email email);

    boolean existsByEmail(Email email);
}
