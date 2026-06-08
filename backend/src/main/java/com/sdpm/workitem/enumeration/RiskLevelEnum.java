package com.sdpm.workitem.enumeration;

public enum RiskLevelEnum {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    private final String code;
    private final String desc;

    RiskLevelEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static RiskLevelEnum fromCode(String code) {
        for (RiskLevelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown RiskLevelEnum code: " + code);
    }
}