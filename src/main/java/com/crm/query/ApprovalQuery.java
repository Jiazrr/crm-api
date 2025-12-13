package com.crm.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalQuery {
    @NotNull(message = "审核id不能为空")
    private Integer id;
    @NotNull(message = "审核类型不能为空")
    private Integer type;
    @NotBlank(message = "审核意见不能为空")
    private String comment; // 新增：审核意见（自定义原因）
}
