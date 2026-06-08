package com.sdpm.workitem.enumeration;

public enum WorkItemStatusEnum {
    DRAFT("草稿"),
    ANALYZING("待分析"),
    READY("待开发"),
    IN_PROGRESS("开发中"),
    IN_TESTING("测试中"),
    DONE("已完成");

    private final String code;
    private final String desc;

    WorkItemStatusEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WorkItemStatusEnum fromCode(String code) {
        for (WorkItemStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown WorkItemStatusEnum code: " + code);
    }
}