package com.sdpm.workitem.enumeration;

public enum ClarificationSeverityEnum {
    P0("P0-阻断"),
    P1("P1-重要"),
    P2("P2-建议");

    private final String code;
    private final String desc;

    ClarificationSeverityEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ClarificationSeverityEnum fromCode(String code) {
        for (ClarificationSeverityEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown ClarificationSeverityEnum code: " + code);
    }
}