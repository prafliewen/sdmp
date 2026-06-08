package com.sdpm.workitem.common;

public class ErrorCode {

    private final int code;
    private final String message;

    public ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    // ========== 业务错误码 (400-499) ==========
    public static final ErrorCode BIZ_PARAM_INVALID = new ErrorCode(400, "参数校验失败");

    // ========== 业务错误码 (404) ==========
    public static final ErrorCode BIZ_NOT_FOUND = new ErrorCode(404, "资源不存在");

    // ========== 业务错误码 (409 冲突类) ==========
    public static final ErrorCode BIZ_DUPLICATE_CODE = new ErrorCode(409, "业务编码重复");
    public static final ErrorCode BIZ_DUPLICATE_QUESTION = new ErrorCode(409, "同一工作项下已存在相同澄清问题");
    public static final ErrorCode BIZ_DUPLICATE_DICT_KEY = new ErrorCode(409, "字典类型与Key重复");
    public static final ErrorCode BIZ_VERSION_CONFLICT = new ErrorCode(409, "数据已被他人修改，请刷新后重试");
    public static final ErrorCode BIZ_TRANSITION_NOT_ALLOWED = new ErrorCode(409, "状态流转不合法");
    public static final ErrorCode BIZ_DONE_IMMUTABLE = new ErrorCode(409, "已完成的工作项不可再次变更");
    public static final ErrorCode BIZ_P0_CLARIFICATION_BLOCKED = new ErrorCode(409, "存在未解决的P0澄清问题，阻断状态流转");
    public static final ErrorCode BIZ_CLARIFICATION_ALREADY_RESOLVED = new ErrorCode(409, "该澄清问题已解决");
    public static final ErrorCode BIZ_DICT_IN_USE = new ErrorCode(409, "字典项被引用，无法删除");
    public static final ErrorCode BIZ_FORBIDDEN = new ErrorCode(403, "权限不足");

    // ========== AI 相关 (400-502) ==========
    public static final ErrorCode BIZ_AI_CAPABILITY_NOT_FOUND = new ErrorCode(400, "不支持的AI分析类型");
    public static final ErrorCode BIZ_AI_SCHEMA_INVALID = new ErrorCode(500, "AI返回结果不符合预期结构");
    public static final ErrorCode BIZ_AI_UPSTREAM_FAILURE = new ErrorCode(502, "上游AI服务不可用");

    // ========== 系统错误码 (500) ==========
    public static final ErrorCode SYS_INTERNAL = new ErrorCode(500, "系统繁忙，请稍后再试");
}