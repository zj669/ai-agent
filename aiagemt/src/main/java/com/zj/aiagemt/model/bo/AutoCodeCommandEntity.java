package com.zj.aiagemt.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoCodeCommandEntity {
    private String aiAgentId;
    private List<String> diff;
}
