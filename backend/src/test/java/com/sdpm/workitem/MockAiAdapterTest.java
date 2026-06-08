package com.sdpm.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.ai.adapter.MockAiAdapter;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockAiAdapterTest {

    private MockAiAdapter adapter;
    private WorkItemEntity workItem;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adapter = new MockAiAdapter();
        workItem = new WorkItemEntity();
        workItem.setId(1L);
        workItem.setTitle("测试工作项");
        workItem.setDescription("这是一个测试工作项的描述");
        workItem.setType("STORY");
    }

    @Test
    @DisplayName("SUMMARY: 返回结构化结果含必要字段")
    void shouldReturnStructuredSummary() {
        Map<String, Object> result = adapter.execute(AiAnalysisTypeEnum.SUMMARY, workItem);
        assertNotNull(result);
        assertTrue(result.containsKey("headline"));
        assertTrue(result.containsKey("background"));
        assertTrue(result.containsKey("goal"));
        assertTrue(result.containsKey("scope"));
        assertTrue(result.containsKey("risks"));
        assertTrue(result.containsKey("keyPoints"));
        assertTrue(((String) result.get("headline")).length() > 0);
    }

    @Test
    @DisplayName("RISK: 返回结构化结果含必要字段")
    void shouldReturnStructuredRisk() {
        Map<String, Object> result = adapter.execute(AiAnalysisTypeEnum.RISK, workItem);
        assertNotNull(result);
        assertTrue(result.containsKey("level"));
        assertTrue(result.containsKey("items"));
        assertNotNull(result.get("items"));
    }

    @Test
    @DisplayName("CLARIFICATION: 返回结构化结果含必要字段")
    void shouldReturnStructuredClarification() {
        Map<String, Object> result = adapter.execute(AiAnalysisTypeEnum.CLARIFICATION, workItem);
        assertNotNull(result);
        assertTrue(result.containsKey("questions"));
        assertNotNull(result.get("questions"));
    }

    @Test
    @DisplayName("ACCEPTANCE: 返回结构化结果含必要字段")
    void shouldReturnStructuredAcceptance() {
        Map<String, Object> result = adapter.execute(AiAnalysisTypeEnum.ACCEPTANCE, workItem);
        assertNotNull(result);
        assertTrue(result.containsKey("criteria"));
        assertTrue(result.containsKey("coverage"));
    }

    @Test
    @DisplayName("TASK_BREAKDOWN: 返回结构化结果含必要字段")
    void shouldReturnStructuredTaskBreakdown() {
        Map<String, Object> result = adapter.execute(AiAnalysisTypeEnum.TASK_BREAKDOWN, workItem);
        assertNotNull(result);
        assertTrue(result.containsKey("tasks"));
        assertTrue(result.containsKey("totalEstimateHours"));
    }

    @Test
    @DisplayName("三种主要能力均返回非空结果")
    void allMainCapabilitiesReturnNonNull() {
        for (AiAnalysisTypeEnum type : new AiAnalysisTypeEnum[]{
                AiAnalysisTypeEnum.SUMMARY,
                AiAnalysisTypeEnum.RISK,
                AiAnalysisTypeEnum.CLARIFICATION}) {
            Map<String, Object> result = adapter.execute(type, workItem);
            assertNotNull(result, type.getCode() + " 不应返回null");
            assertFalse(result.isEmpty(), type.getCode() + " 不应返回空Map");
        }
    }
}