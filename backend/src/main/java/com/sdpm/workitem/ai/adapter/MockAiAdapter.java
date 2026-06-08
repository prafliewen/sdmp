package com.sdpm.workitem.ai.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MockAiAdapter implements AiAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<AiAnalysisTypeEnum, Map<String, Object>> mockResponses;

    public MockAiAdapter() {
        mockResponses = new LinkedHashMap<>();
        mockResponses.put(AiAnalysisTypeEnum.SUMMARY, buildSummary());
        mockResponses.put(AiAnalysisTypeEnum.RISK, buildRisk());
        mockResponses.put(AiAnalysisTypeEnum.CLARIFICATION, buildClarification());
        mockResponses.put(AiAnalysisTypeEnum.ACCEPTANCE, buildAcceptance());
        mockResponses.put(AiAnalysisTypeEnum.TASK_BREAKDOWN, buildTaskBreakdown());
    }

    @Override
    public Map<String, Object> execute(AiAnalysisTypeEnum type, WorkItemEntity workItem) {
        Map<String, Object> response = mockResponses.get(type);
        if (response == null) {
            return Collections.emptyMap();
        }
        return deepCopy(response);
    }

    private Map<String, Object> deepCopy(Map<String, Object> source) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(source);
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>(source);
        }
    }

    private Map<String, Object> buildSummary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("headline", "需求摘要");
        map.put("background", "背景说明");
        map.put("goal", "目标");
        map.put("scope", Arrays.asList("范围1", "范围2"));
        map.put("risks", Collections.singletonList("风险1"));
        map.put("keyPoints", Arrays.asList("要点1", "要点2"));
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRisk() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("level", "MEDIUM");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "技术风险");
        item.put("desc", "描述");
        item.put("mitigation", "缓解措施");
        map.put("items", Collections.singletonList(item));
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildClarification() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("question", "需要澄清的问题");
        question.put("severity", "P1");
        question.put("reason", "原因");
        map.put("questions", Collections.singletonList(question));
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildAcceptance() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> criterion = new LinkedHashMap<>();
        criterion.put("given", "前提");
        criterion.put("when", "当");
        criterion.put("then", "则");
        map.put("criteria", Collections.singletonList(criterion));
        map.put("coverage", "覆盖维度");
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTaskBreakdown() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("title", "任务");
        task.put("estimateHours", 4.0);
        task.put("ownerHint", "backend");
        map.put("tasks", Collections.singletonList(task));
        map.put("totalEstimateHours", 8.0);
        return map;
    }
}