package com.sdpm.workitem.enumeration;

public enum AiAnalysisTypeEnum {
    SUMMARY("需求摘要"),
    ACCEPTANCE("验收标准"),
    RISK("风险识别"),
    CLARIFICATION("澄清问题"),
    TASK_BREAKDOWN("任务拆解");

    private final String code;
    private final String desc;

    AiAnalysisTypeEnum(String desc) {
        this.code = this.name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AiAnalysisTypeEnum fromCode(String code) {
        for (AiAnalysisTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown AiAnalysisTypeEnum code: " + code);
    }
}