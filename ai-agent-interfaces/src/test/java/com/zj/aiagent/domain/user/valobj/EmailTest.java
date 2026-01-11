package com.zj.aiagent.domain.user.valobj;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Email 值对象测试")
class EmailTest {

    @Test
    @DisplayName("有效邮箱应创建成功")
    void shouldCreateEmailWithValidAddress() {
        // When
        Email email = Email.of("user@example.com");

        // Then
        assertNotNull(email);
        assertEquals("user@example.com", email.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test@domain.com",
            "user.name@example.org",
            "user+tag@gmail.com",
            "test123@sub.domain.co.uk"
    })
    @DisplayName("多种有效邮箱格式应全部通过")
    void shouldAcceptVariousValidEmailFormats(String emailStr) {
        // When & Then
        assertDoesNotThrow(() -> Email.of(emailStr));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "invalid",
            "no-at-sign.com",
            "@no-local-part.com",
            "spaces in@email.com"
    })
    @DisplayName("无效邮箱格式应抛出异常")
    void shouldThrowExceptionForInvalidEmail(String invalidEmail) {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> Email.of(invalidEmail));
    }

    @Test
    @DisplayName("null 邮箱应抛出异常")
    void shouldThrowExceptionForNullEmail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> Email.of(null));
    }

    @Test
    @DisplayName("相同值的 Email 对象应相等")
    void shouldBeEqualWhenSameValue() {
        // Given
        Email email1 = Email.of("test@example.com");
        Email email2 = Email.of("test@example.com");

        // Then
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }
}
