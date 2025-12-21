package com.zj.aiagent.application.user.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取用户信息查询
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserInfoQuery {

    /**
     * 用户ID
     */
    private Long userId;
}
