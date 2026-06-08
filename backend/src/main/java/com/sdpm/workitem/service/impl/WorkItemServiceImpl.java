package com.sdpm.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.config.UserContext;
import com.sdpm.workitem.dto.WorkItemCreateReqDTO;
import com.sdpm.workitem.dto.WorkItemQueryReqDTO;
import com.sdpm.workitem.dto.WorkItemUpdateReqDTO;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.entity.WorkItemStatusHistoryEntity;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.mapper.WorkItemStatusHistoryMapper;
import com.sdpm.workitem.service.WorkItemService;
import com.sdpm.workitem.vo.WorkItemDetailRespVO;
import com.sdpm.workitem.vo.WorkItemRespVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkItemServiceImpl implements WorkItemService {

    @Autowired
    private WorkItemMapper workItemMapper;

    @Autowired
    private WorkItemStatusHistoryMapper workItemStatusHistoryMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AtomicLong CODE_SEQUENCE = new AtomicLong(0);
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public WorkItemRespVO createWorkItem(WorkItemCreateReqDTO req) {
        WorkItemEntity entity = new WorkItemEntity();
        entity.setTitle(req.getTitle());

        String code = req.getCode();
        if (!StringUtils.hasText(code)) {
            code = generateCode();
        }
        entity.setCode(code);

        entity.setDescription(req.getDescription());
        entity.setType(req.getType());
        entity.setPriority(req.getPriority());
        entity.setAssignee(req.getAssignee());
        entity.setReporter(req.getReporter());

        if (req.getTags() != null && !req.getTags().isEmpty()) {
            entity.setTags(toJson(req.getTags()));
        }
        if (req.getAcceptanceCriteria() != null && !req.getAcceptanceCriteria().isEmpty()) {
            entity.setAcceptanceCriteria(toJson(req.getAcceptanceCriteria()));
        }

        entity.setStatus("DRAFT");

        workItemMapper.insert(entity);

        WorkItemStatusHistoryEntity history = new WorkItemStatusHistoryEntity();
        history.setWorkItemId(entity.getId());
        history.setFromStatus(null);
        history.setToStatus("DRAFT");
        history.setReason("创建工作项");
        history.setOperator(UserContext.getOperator());
        workItemStatusHistoryMapper.insert(history);

        return toRespVO(entity);
    }

    @Override
    @Transactional
    public WorkItemRespVO updateWorkItem(Long id, WorkItemUpdateReqDTO req) {
        WorkItemEntity entity = workItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        if ("DONE".equals(entity.getStatus())) {
            throw new BizException(ErrorCode.BIZ_DONE_IMMUTABLE);
        }

        entity.setVersion(req.getVersion());

        if (StringUtils.hasText(req.getTitle())) {
            entity.setTitle(req.getTitle());
        }
        if (StringUtils.hasText(req.getDescription())) {
            entity.setDescription(req.getDescription());
        }
        if (StringUtils.hasText(req.getPriority())) {
            entity.setPriority(req.getPriority());
        }
        if (req.getAssignee() != null) {
            entity.setAssignee(req.getAssignee());
        }
        if (req.getTags() != null) {
            entity.setTags(req.getTags().isEmpty() ? null : toJson(req.getTags()));
        }
        if (req.getAcceptanceCriteria() != null) {
            entity.setAcceptanceCriteria(req.getAcceptanceCriteria().isEmpty() ? null : toJson(req.getAcceptanceCriteria()));
        }

        int rows = workItemMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.BIZ_VERSION_CONFLICT);
        }

        return toRespVO(entity);
    }

    @Override
    public WorkItemDetailRespVO getWorkItemDetail(Long id) {
        WorkItemEntity entity = workItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        WorkItemDetailRespVO vo = new WorkItemDetailRespVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setType(entity.getType());
        vo.setPriority(entity.getPriority());
        vo.setStatus(entity.getStatus());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setAssignee(entity.getAssignee());
        vo.setReporter(entity.getReporter());
        vo.setTags(parseJsonList(entity.getTags()));
        vo.setAcceptanceCriteria(parseJsonList(entity.getAcceptanceCriteria()));
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        // TODO: Clarification module not built yet. Hardcode counts to 0.
        vo.setP0OpenClarifications(0);
        vo.setTotalOpenClarifications(0);

        // TODO: getLastTransitionTime - implement when workflow transition logic is available
        vo.setLastTransitionTime(null);

        return vo;
    }

    @Override
    public PageResp<WorkItemRespVO> pageWorkItems(WorkItemQueryReqDTO req) {
        LambdaQueryWrapper<WorkItemEntity> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(WorkItemEntity::getTitle, req.getKeyword())
                    .or()
                    .like(WorkItemEntity::getCode, req.getKeyword()));
        }
        if (StringUtils.hasText(req.getType())) {
            wrapper.eq(WorkItemEntity::getType, req.getType());
        }
        if (StringUtils.hasText(req.getPriority())) {
            wrapper.eq(WorkItemEntity::getPriority, req.getPriority());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WorkItemEntity::getStatus, req.getStatus());
        }
        if (StringUtils.hasText(req.getAssignee())) {
            wrapper.eq(WorkItemEntity::getAssignee, req.getAssignee());
        }
        if (StringUtils.hasText(req.getReporter())) {
            wrapper.eq(WorkItemEntity::getReporter, req.getReporter());
        }

        String sortBy = req.getSortBy();
        String sortDir = req.getSortDir();
        boolean isAsc = "asc".equalsIgnoreCase(sortDir);

        switch (sortBy != null ? sortBy : "createdAt") {
            case "updatedAt":
                wrapper.orderBy(true, isAsc, WorkItemEntity::getUpdatedAt);
                break;
            case "priority":
                wrapper.orderBy(true, isAsc, WorkItemEntity::getPriority);
                break;
            default:
                wrapper.orderBy(true, isAsc, WorkItemEntity::getCreatedAt);
                break;
        }

        Page<WorkItemEntity> page = new Page<>(req.getPageNo(), req.getPageSize());
        IPage<WorkItemEntity> result = workItemMapper.selectPage(page, wrapper);

        List<WorkItemRespVO> records = result.getRecords().stream()
                .map(this::toRespVO)
                .toList();

        return PageResp.of(
                req.getPageNo(),
                req.getPageSize(),
                result.getTotal(),
                records);
    }

    @Override
    @Transactional
    public void softDeleteWorkItem(Long id) {
        WorkItemEntity entity = workItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        if ("DONE".equals(entity.getStatus())) {
            throw new BizException(ErrorCode.BIZ_DONE_IMMUTABLE);
        }

        entity.setDeleted(1);
        workItemMapper.updateById(entity);
    }

    private WorkItemRespVO toRespVO(WorkItemEntity entity) {
        WorkItemRespVO vo = new WorkItemRespVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setType(entity.getType());
        vo.setPriority(entity.getPriority());
        vo.setStatus(entity.getStatus());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setAssignee(entity.getAssignee());
        vo.setReporter(entity.getReporter());
        vo.setTags(parseJsonList(entity.getTags()));
        vo.setAcceptanceCriteria(parseJsonList(entity.getAcceptanceCriteria()));
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String generateCode() {
        String date = LocalDate.now().format(CODE_DATE_FORMAT);
        long seq = CODE_SEQUENCE.incrementAndGet();
        return "WI-" + date + "-" + String.format("%04d", seq);
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    private List<String> parseJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}