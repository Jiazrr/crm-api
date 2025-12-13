package com.crm.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 合同审核状态枚举（匹配图片定义：0-初始化；1-同意；2-拒绝）
 */
@Getter
public enum ContractStatusEnum {
    INIT(0, "初始化"),
    AUDITING(1, "审核中"),
    APPROVED(2, "审核通过"),
    REJECTED(3, "审核拒绝");

    @EnumValue // MyBatis-Plus映射数据库数值
    private final Integer code;
    private final String desc;

    ContractStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据code获取枚举（方便业务逻辑调用）
    public static ContractStatusEnum getByCode(Integer code) {
        for (ContractStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return INIT; // 默认返回初始化状态
    }
}
