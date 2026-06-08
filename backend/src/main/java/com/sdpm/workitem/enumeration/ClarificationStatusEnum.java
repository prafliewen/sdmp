package com.sdpm.workitem.enumeration;

public enum ClarificationStatusEnum {
    OPEN("未解决"),
    RESOLVED("已解决");

    private final String code;
    private final String desc;

    ClarificationStatusEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ClarificationStatusEnum fromCode(String code) {
        for (ClarificationStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown ClarificationStatusEnum code: " + code);
    }
}