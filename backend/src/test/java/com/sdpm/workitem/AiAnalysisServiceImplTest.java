package com.sdpm.workitem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdpm.workitem.ai.AiCapability;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.entity.AiAnalysisResultEntity;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.AiAnalysisResultMapper;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.service.impl.AiAnalysisServiceImpl;
import com.sdpm.workitem.vo.AiAnalysisRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceImplTest {

    @Mock
    private WorkItemMapper workItemMapper;

    @Mock
    private AiAnalysisResultMapper aiAnalysisResultMapper;

    @Mock
    private AiCapability summaryCapability;

    @Mock
    private AiCapability riskCapability;

    @InjectMocks
    private AiAnalysisServiceImpl aiAnalysisService;

    private WorkItemEntity workItem;

    @BeforeEach
    void setUp() {
        workItem = new WorkItemEntity();
        workItem.setId(1L);
        workItem.setTitle("测试工作项");
        workItem.setDescription("测试描述");
        workItem.setType("STORY");

        // @InjectMocks doesn't handle List<AiCapability> properly
        ReflectionTestUtils.setField(aiAnalysisService, "capabilities",
                Arrays.asList(summaryCapability, riskCapability));
    }

    private void stubCapabilities() {
        lenient().when(summaryCapability.supports()).thenReturn(AiAnalysisTypeEnum.SUMMARY);
        lenient().when(riskCapability.supports()).thenReturn(AiAnalysisTypeEnum.RISK);
    }

    // ========== 触发分析 ==========
    @Test
    @DisplayName("触发SUMMARY分析成功")
    void shouldTriggerSummaryAnalysis() {
        stubCapabilities();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("headline", "需求摘要标题");
        payload.put("background", "背景");

        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(summaryCapability.analyse(workItem)).thenReturn(payload);
        when(aiAnalysisResultMapper.insert(any(AiAnalysisResultEntity.class))).thenReturn(1);

        AiAnalysisRespVO result = aiAnalysisService.triggerAnalysis(1L, "SUMMARY");

        assertNotNull(result);
        assertEquals("SUMMARY", result.getAnalysisType());
        assertEquals("需求摘要标题", result.getSummary());
        verify(aiAnalysisResultMapper).insert(any(AiAnalysisResultEntity.class));
    }

    @Test
    @DisplayName("触发RISK分析成功")
    void shouldTriggerRiskAnalysis() {
        stubCapabilities();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("level", "HIGH");
        payload.put("items", Collections.emptyList());

        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(riskCapability.analyse(workItem)).thenReturn(payload);
        when(aiAnalysisResultMapper.insert(any(AiAnalysisResultEntity.class))).thenReturn(1);

        AiAnalysisRespVO result = aiAnalysisService.triggerAnalysis(1L, "RISK");

        assertNotNull(result);
        assertEquals("RISK", result.getAnalysisType());
        assertEquals("风险等级: HIGH", result.getSummary());
    }

    @Test
    @DisplayName("分析不存在的工作项 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnAnalysis() {
        when(workItemMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> aiAnalysisService.triggerAnalysis(999L, "SUMMARY"));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("不支持的AI分析类型 → 抛BIZ_AI_CAPABILITY_NOT_FOUND")
    void shouldThrowOnInvalidAnalysisType() {
        when(workItemMapper.selectById(1L)).thenReturn(workItem);

        BizException ex = assertThrows(BizException.class,
                () -> aiAnalysisService.triggerAnalysis(1L, "INVALID_TYPE"));
        assertEquals(ErrorCode.BIZ_AI_CAPABILITY_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("AI返回空结果 → 抛BIZ_AI_SCHEMA_INVALID")
    void shouldThrowOnEmptyResult() {
        stubCapabilities();
        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(summaryCapability.analyse(workItem)).thenReturn(Collections.emptyMap());

        BizException ex = assertThrows(BizException.class,
                () -> aiAnalysisService.triggerAnalysis(1L, "SUMMARY"));
        assertEquals(ErrorCode.BIZ_AI_SCHEMA_INVALID.getCode(), ex.getErrorCode().getCode());
    }

    // ========== 查询分析记录 ==========
    @Test
    @DisplayName("查询分析记录列表")
    void shouldListAnalyses() {
        AiAnalysisResultEntity entity = new AiAnalysisResultEntity();
        entity.setId(1L);
        entity.setWorkItemId(1L);
        entity.setAnalysisType("SUMMARY");
        entity.setPayload("{\"headline\":\"摘要\"}");
        entity.setSource("MOCK");
        entity.setCreatedAt(java.time.LocalDateTime.now());

        Page<AiAnalysisResultEntity> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(entity));
        page.setTotal(1L);

        when(aiAnalysisResultMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResp<AiAnalysisRespVO> result = aiAnalysisService.listAnalyses(1L, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("SUMMARY", result.getRecords().get(0).getAnalysisType());
    }

    @Test
    @DisplayName("空分析记录列表")
    void shouldReturnEmptyAnalysisList() {
        Page<AiAnalysisResultEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);

        when(aiAnalysisResultMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResp<AiAnalysisRespVO> result = aiAnalysisService.listAnalyses(1L, null, 1, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }
}