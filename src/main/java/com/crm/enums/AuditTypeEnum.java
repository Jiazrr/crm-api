package com.crm.enums;

import lombok.Getter;

@Getter
public enum AuditTypeEnum {
    APPROVE(0, "审核通过"),
    REJECT(1, "审核拒绝");

    private final Integer code;
    private final String desc;

    AuditTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
