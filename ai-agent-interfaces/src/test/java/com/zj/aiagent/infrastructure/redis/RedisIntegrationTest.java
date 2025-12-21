package com.zj.aiagent.infrastructure.redis;

import com.zj.aiagent.shared.constants.RedisKeyConstants;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("dev")
class RedisIntegrationTest {

    @Autowired
    private IRedisService redisService;

    @Test
    void testRedissonClientNotNull() {
        // 验证RedissonClient已成功注入
        assertNotNull(redisService, "RedissonClient应该被成功注入");
    }

    @Test
    void testBasicStringOperations() {
        // 测试基本的字符串操作
        String key = "test:string:key";
        String value = "Hello Redis!";

        redisService.setValue(key, value);
        // 验证存储和读取
        String retrieved = redisService.getValue(key);
        assertEquals(value, retrieved, "存储的值应该能够被正确读取");
        // 清理测试数据
        redisService.remove(key);
    }


}
