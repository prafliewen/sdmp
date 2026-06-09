package com.sdpm.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.ai.AiCapability;
import com.sdpm.workitem.ai.adapter.AiAdapterRouter;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.entity.AiAnalysisResultEntity;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.AiAnalysisResultMapper;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.service.AiAnalysisService;
import com.sdpm.workitem.vo.AiAnalysisRespVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    @Autowired
    private WorkItemMapper workItemMapper;

    @Autowired
    private AiAnalysisResultMapper aiAnalysisResultMapper;

    @Autowired
    private List<AiCapability> capabilities;

    @Autowired
    private AiAdapterRouter aiAdapterRouter;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public AiAnalysisRespVO triggerAnalysis(Long workItemId, String analysisType) {
        WorkItemEntity workItem = workItemMapper.selectById(workItemId);
        if (workItem == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        AiAnalysisTypeEnum typeEnum;
        try {
            typeEnum = AiAnalysisTypeEnum.fromCode(analysisType);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BIZ_AI_CAPABILITY_NOT_FOUND);
        }

        Map<AiAnalysisTypeEnum, AiCapability> capabilityMap = capabilities.stream()
                .collect(Collectors.toMap(AiCapability::supports, c -> c, (a, b) -> a));

        AiCapability capability = capabilityMap.get(typeEnum);
        if (capability == null) {
            throw new BizException(ErrorCode.BIZ_AI_CAPABILITY_NOT_FOUND);
        }

        Map<String, Object> payload = capability.analyse(workItem);
        if (payload == null || payload.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_AI_SCHEMA_INVALID);
        }

        String payloadJson;
        try {
            payloadJson = OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.BIZ_AI_SCHEMA_INVALID, "payload序列化失败");
        }

        AiAnalysisResultEntity entity = new AiAnalysisResultEntity();
        entity.setWorkItemId(workItemId);
        entity.setAnalysisType(typeEnum.getCode());
        entity.setPayload(payloadJson);
        entity.setSource(aiAdapterRouter.activeSource().getCode());
        entity.setCreatedAt(LocalDateTime.now());

        aiAnalysisResultMapper.insert(entity);

        String summary = generateSummary(payload);

        AiAnalysisRespVO vo = new AiAnalysisRespVO();
        vo.setId(entity.getId());
        vo.setWorkItemId(entity.getWorkItemId());
        vo.setAnalysisType(entity.getAnalysisType());
        vo.setSource(entity.getSource());
        vo.setPayload(payload);
        vo.setSummary(summary);
        vo.setCreatedAt(entity.getCreatedAt().format(DATE_TIME_FORMATTER));
        return vo;
    }

    @Override
    public PageResp<AiAnalysisRespVO> listAnalyses(Long workItemId, String analysisType, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<AiAnalysisResultEntity> query = new LambdaQueryWrapper<>();
        query.eq(AiAnalysisResultEntity::getWorkItemId, workItemId);
        if (StringUtils.hasText(analysisType)) {
            query.eq(AiAnalysisResultEntity::getAnalysisType, analysisType);
        }
        query.orderByDesc(AiAnalysisResultEntity::getCreatedAt);

        IPage<AiAnalysisResultEntity> page = aiAnalysisResultMapper.selectPage(
                new Page<>(pageNo, pageSize), query);

        List<AiAnalysisRespVO> records = page.getRecords().stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());

        return PageResp.of(pageNo, pageSize, page.getTotal(), records);
    }

    private String generateSummary(Map<String, Object> payload) {
        if (payload.containsKey("headline")) {
            Object headline = payload.get("headline");
            return headline != null ? headline.toString() : "";
        }
        if (payload.containsKey("level")) {
            Object level = payload.get("level");
            return "风险等级: " + (level != null ? level.toString() : "");
        }
        if (payload.containsKey("coverage")) {
            Object coverage = payload.get("coverage");
            return "验收覆盖: " + (coverage != null ? coverage.toString() : "");
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String && StringUtils.hasText((String) value)) {
                return (String) value;
            }
        }
        return "";
    }

    private AiAnalysisRespVO toRespVO(AiAnalysisResultEntity entity) {
        AiAnalysisRespVO vo = new AiAnalysisRespVO();
        vo.setId(entity.getId());
        vo.setWorkItemId(entity.getWorkItemId());
        vo.setAnalysisType(entity.getAnalysisType());
        vo.setSource(entity.getSource());

        try {
            Map<String, Object> payload = OBJECT_MAPPER.readValue(
                    entity.getPayload(), new TypeReference<Map<String, Object>>() {});
            vo.setPayload(payload);
            vo.setSummary(generateSummary(payload));
        } catch (JsonProcessingException e) {
            vo.setPayload(new HashMap<>());
            vo.setSummary("");
        }

        if (entity.getCreatedAt() != null) {
            vo.setCreatedAt(entity.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
        return vo;
    }
}