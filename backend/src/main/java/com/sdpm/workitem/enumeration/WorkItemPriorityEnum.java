package com.sdpm.workitem.enumeration;

public enum WorkItemPriorityEnum {
    P0("P0-紧急"),
    P1("P1-高"),
    P2("P2-中"),
    P3("P3-低");

    private final String code;
    private final String desc;

    WorkItemPriorityEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WorkItemPriorityEnum fromCode(String code) {
        for (WorkItemPriorityEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown WorkItemPriorityEnum code: " + code);
    }
}