package com.sdpm.workitem.enumeration;

public enum WorkItemTypeEnum {
    STORY("需求"),
    BUG("缺陷"),
    TASK("任务");

    private final String code;
    private final String desc;

    WorkItemTypeEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WorkItemTypeEnum fromCode(String code) {
        for (WorkItemTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown WorkItemTypeEnum code: " + code);
    }
}