package com.zj.aiagent.domain.user;

import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.valobj.Credential;
import com.zj.aiagent.domain.user.valobj.Email;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserTest {

    @Test
    public void testCreateUser() {
        Email email = Email.of("test@example.com");
        Credential credential = Credential.create("password123");
        User user = new User("testuser", email, credential);

        Assertions.assertEquals("testuser", user.getUsername());
        Assertions.assertEquals("test@example.com", user.getEmail().getValue());
        Assertions.assertTrue(user.verifyPassword("password123"));
        Assertions.assertFalse(user.verifyPassword("wrongpass"));
    }

    @Test
    public void testModifyInfo() {
        User user = new User("oldname", Email.of("a@b.com"), Credential.create("123"));
        user.modifyInfo("newname", "http://avatar.com/1.png", "13800000000");

        Assertions.assertEquals("newname", user.getUsername());
        Assertions.assertEquals("http://avatar.com/1.png", user.getAvatarUrl());
        Assertions.assertEquals("13800000000", user.getPhone());
    }
}
