package com.zj.aiagent.domain.user.valobj;

import com.zj.aiagent.shared.util.BCryptUtil;
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

        // When: 在领域服务层使用 BCryptUtil.encode，再构建 Credential
        String encrypted = BCryptUtil.encode(rawPassword);
        Credential credential = Credential.fromEncrypted(encrypted);

        // Then
        assertNotNull(credential);
        assertNotNull(credential.getEncryptedPassword());
        assertNotEquals(rawPassword, credential.getEncryptedPassword());
        assertTrue(credential.getEncryptedPassword().startsWith("$2a$") || credential.getEncryptedPassword().startsWith("$2b$"));
    }

    @Test
    @DisplayName("正确密码应验证成功")
    void shouldVerifyCorrectPassword() {
        // Given
        String rawPassword = "correctPassword123";
        String encrypted = BCryptUtil.encode(rawPassword);
        Credential credential = Credential.fromEncrypted(encrypted);

        // When
        boolean result = credential.verify(rawPassword);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("错误密码应验证失败")
    void shouldNotVerifyIncorrectPassword() {
        // Given
        String encrypted = BCryptUtil.encode("correctPassword");
        Credential credential = Credential.fromEncrypted(encrypted);

        // When
        boolean result = credential.verify("wrongPassword");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("空密码应验证失败")
    void shouldNotVerifyEmptyPassword() {
        // Given
        String encrypted = BCryptUtil.encode("somePassword");
        Credential credential = Credential.fromEncrypted(encrypted);

        // When
        boolean result = credential.verify("");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("null 密码应验证失败")
    void shouldNotVerifyNullPassword() {
        // Given
        String encrypted = BCryptUtil.encode("somePassword");
        Credential credential = Credential.fromEncrypted(encrypted);

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
        String encrypted = BCryptUtil.encode(rawPassword);

        // 模拟从数据库重建
        Credential reconstructed = Credential.fromEncrypted(encrypted);

        // When
        boolean result = reconstructed.verify(rawPassword);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("空加密密码应抛出异常")
    void shouldThrowExceptionForEmptyEncryptedPassword() {
        assertThrows(IllegalArgumentException.class, () -> Credential.fromEncrypted(""));
        assertThrows(IllegalArgumentException.class, () -> Credential.fromEncrypted(null));
        assertThrows(IllegalArgumentException.class, () -> Credential.fromEncrypted("   "));
    }
}
