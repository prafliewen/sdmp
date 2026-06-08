package com.sdpm.workitem.enumeration;

public enum AiSourceEnum {
    MOCK("模拟"),
    LLM("大模型");

    private final String code;
    private final String desc;

    AiSourceEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AiSourceEnum fromCode(String code) {
        for (AiSourceEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown AiSourceEnum code: " + code);
    }
}