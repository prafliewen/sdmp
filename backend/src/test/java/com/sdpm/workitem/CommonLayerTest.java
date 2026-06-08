package com.sdpm.workitem;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;
import com.sdpm.workitem.enumeration.ClarificationSeverityEnum;
import com.sdpm.workitem.enumeration.ClarificationStatusEnum;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;

class CommonLayerTest {

    // ========== Result ==========
    @Test
    @DisplayName("Result.success() 返回 code=0")
    void resultSuccessHasCodeZero() {
        Result<String> r = Result.success("data");
        assertEquals(0, r.getCode());
        assertEquals("success", r.getMessage());
        assertEquals("data", r.getData());
        assertNotNull(r.getTimestamp());
    }

    @Test
    @DisplayName("Result.success(null) 返回空data")
    void resultSuccessNullData() {
        Result<Object> r = Result.success();
        assertEquals(0, r.getCode());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("Result.error(ErrorCode) 返回对应错误码")
    void resultErrorFromErrorCode() {
        Result<Void> r = Result.error(ErrorCode.BIZ_NOT_FOUND);
        assertEquals(404, r.getCode());
        assertEquals("资源不存在", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("Result.error(ErrorCode, detail) 返回自定义message")
    void resultErrorWithDetailMessage() {
        Result<Void> r = Result.error(ErrorCode.BIZ_NOT_FOUND, "工作项ID=999");
        assertEquals(404, r.getCode());
        assertEquals("工作项ID=999", r.getMessage());
    }

    // ========== PageResp ==========
    @Test
    @DisplayName("PageResp.of() 构建分页结果")
    void pageRespBuildCorrectly() {
        List<String> records = Arrays.asList("a", "b", "c");
        PageResp<String> p = PageResp.of(1, 10, 3L, records);

        assertEquals(Integer.valueOf(1), p.getPageNo());
        assertEquals(Integer.valueOf(10), p.getPageSize());
        assertEquals(Long.valueOf(3L), p.getTotal());
        assertEquals(3, p.getRecords().size());
        assertEquals("a", p.getRecords().get(0));
    }

    // ========== BizException ==========
    @Test
    @DisplayName("BizException 携带 ErrorCode")
    void bizExceptionCarriesErrorCode() {
        BizException ex = new BizException(ErrorCode.BIZ_NOT_FOUND);
        assertEquals(ErrorCode.BIZ_NOT_FOUND, ex.getErrorCode());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    @DisplayName("BizException 携带自定义message")
    void bizExceptionWithCustomMessage() {
        BizException ex = new BizException(ErrorCode.BIZ_PARAM_INVALID, "字段A不能为空");
        assertEquals(ErrorCode.BIZ_PARAM_INVALID, ex.getErrorCode());
        assertEquals("字段A不能为空", ex.getMessage());
    }

    // ========== WorkItemStatusEnum ==========
    @Test
    @DisplayName("WorkItemStatusEnum.fromCode() 正常解析")
    void workItemStatusEnumFromCode() {
        assertEquals(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.fromCode("DRAFT"));
        assertEquals(WorkItemStatusEnum.DONE, WorkItemStatusEnum.fromCode("DONE"));
    }

    @Test
    @DisplayName("WorkItemStatusEnum.fromCode() 非法值抛异常")
    void workItemStatusEnumInvalidCode() {
        assertThrows(IllegalArgumentException.class,
                () -> WorkItemStatusEnum.fromCode("INVALID"));
    }

    @Test
    @DisplayName("WorkItemStatusEnum.getDesc() 返回中文描述")
    void workItemStatusEnumDesc() {
        assertEquals("草稿", WorkItemStatusEnum.DRAFT.getDesc());
        assertEquals("已完成", WorkItemStatusEnum.DONE.getDesc());
    }

    // ========== ClarificationStatusEnum ==========
    @Test
    @DisplayName("ClarificationStatusEnum.fromCode() 正常解析")
    void clarificationStatusEnumFromCode() {
        assertEquals(ClarificationStatusEnum.OPEN, ClarificationStatusEnum.fromCode("OPEN"));
        assertEquals(ClarificationStatusEnum.RESOLVED, ClarificationStatusEnum.fromCode("RESOLVED"));
    }

    // ========== ClarificationSeverityEnum ==========
    @Test
    @DisplayName("ClarificationSeverityEnum 包含P0/P1/P2")
    void clarificationSeverityEnumValues() {
        assertEquals("P0", ClarificationSeverityEnum.P0.getCode());
        assertEquals("P1", ClarificationSeverityEnum.P1.getCode());
        assertEquals("P2", ClarificationSeverityEnum.P2.getCode());
    }

    // ========== AiAnalysisTypeEnum ==========
    @Test
    @DisplayName("AiAnalysisTypeEnum 包含全部5种类型")
    void aiAnalysisTypeEnumAllTypes() {
        assertEquals(5, AiAnalysisTypeEnum.values().length);
        assertNotNull(AiAnalysisTypeEnum.fromCode("SUMMARY"));
        assertNotNull(AiAnalysisTypeEnum.fromCode("ACCEPTANCE"));
        assertNotNull(AiAnalysisTypeEnum.fromCode("RISK"));
        assertNotNull(AiAnalysisTypeEnum.fromCode("CLARIFICATION"));
        assertNotNull(AiAnalysisTypeEnum.fromCode("TASK_BREAKDOWN"));
    }

    @Test
    @DisplayName("AiAnalysisTypeEnum.fromCode() 非法值抛异常")
    void aiAnalysisTypeEnumInvalidCode() {
        assertThrows(IllegalArgumentException.class,
                () -> AiAnalysisTypeEnum.fromCode("UNKNOWN"));
    }

    // ========== ErrorCode ==========
    @Test
    @DisplayName("ErrorCode 常量值验证")
    void errorCodeConstants() {
        assertEquals(400, ErrorCode.BIZ_PARAM_INVALID.getCode());
        assertEquals(404, ErrorCode.BIZ_NOT_FOUND.getCode());
        assertEquals(409, ErrorCode.BIZ_TRANSITION_NOT_ALLOWED.getCode());
        assertEquals(409, ErrorCode.BIZ_DONE_IMMUTABLE.getCode());
        assertEquals(409, ErrorCode.BIZ_P0_CLARIFICATION_BLOCKED.getCode());
        assertEquals(409, ErrorCode.BIZ_VERSION_CONFLICT.getCode());
        assertEquals(500, ErrorCode.SYS_INTERNAL.getCode());
    }
}