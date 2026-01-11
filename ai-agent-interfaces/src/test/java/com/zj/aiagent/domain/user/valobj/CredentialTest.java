package com.zj.aiagent.domain.user.valobj;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Credential 值对象测试")
class CredentialTest {

    @Test
    @DisplayName("创建凭证应加密密码")
    void shouldEncryptPasswordWhenCreating() {
        // Given
        String rawPassword = "mySecretPassword";

        // When
        Credential credential = Credential.create(rawPassword);

        // Then
        assertNotNull(credential);
        assertNotNull(credential.getEncryptedPassword());
        assertNotEquals(rawPassword, credential.getEncryptedPassword());
        assertTrue(credential.getEncryptedPassword().startsWith("$2a$"));
    }

    @Test
    @DisplayName("正确密码应验证成功")
    void shouldVerifyCorrectPassword() {
        // Given
        String rawPassword = "correctPassword123";
        Credential credential = Credential.create(rawPassword);

        // When
        boolean result = credential.verify(rawPassword);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("错误密码应验证失败")
    void shouldNotVerifyIncorrectPassword() {
        // Given
        Credential credential = Credential.create("correctPassword");

        // When
        boolean result = credential.verify("wrongPassword");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("空密码应验证失败")
    void shouldNotVerifyEmptyPassword() {
        // Given
        Credential credential = Credential.create("somePassword");

        // When
        boolean result = credential.verify("");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("null 密码应验证失败")
    void shouldNotVerifyNullPassword() {
        // Given
        Credential credential = Credential.create("somePassword");

        // When
        boolean result = credential.verify(null);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("从加密密码重建凭证应能验证")
    void shouldVerifyAfterReconstruction() {
        // Given
        String rawPassword = "originalPassword";
        Credential original = Credential.create(rawPassword);

        // 模拟从数据库重建
        Credential reconstructed = Credential.fromEncrypted(original.getEncryptedPassword());

        // When
        boolean result = reconstructed.verify(rawPassword);

        // Then
        assertTrue(result);
    }
}
