package com.zj.aiagent.domain.chat.valobj;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 引用源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {
    private String sourceUrl;
    private String sourceName;
    private String snippet;
    private Integer pageIndex;
}
