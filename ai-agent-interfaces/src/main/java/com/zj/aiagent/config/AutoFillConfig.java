package com.zj.aiagent.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zj.aiagent.shared.utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 自动填充
 */
@Component
public class AutoFillConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = UserContext.getUserId();
        long now = System.currentTimeMillis();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
        this.strictInsertFill(metaObject, "createTime", Long.class, now);
        this.strictUpdateFill(metaObject, "updateTime", Long.class, now);
    }


    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = UserContext.getUserId();
        long now = System.currentTimeMillis();
        // 1. 填充修改人的用户名
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
        // 2. 填充
        this.strictUpdateFill(metaObject, "updateTime", Long.class, now);
    }
}
