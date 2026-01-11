package com.zj.aiagent.domain.user.valobj;

import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.util.regex.Pattern;

/**
 * 邮箱值对象
 */
@Getter
@ToString
public class Email {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

    private final String value;

    public Email(String value) {
        Assert.hasText(value, "Email cannot be empty");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        this.value = value;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
