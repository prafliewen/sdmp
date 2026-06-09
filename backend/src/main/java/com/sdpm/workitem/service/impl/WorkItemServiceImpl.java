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
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.mapper.WorkItemStatusHistoryMapper;
import com.sdpm.workitem.service.ClarificationService;
import com.sdpm.workitem.service.WorkItemService;
import com.sdpm.workitem.vo.WorkItemDetailRespVO;
import com.sdpm.workitem.vo.WorkItemRespVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class WorkItemServiceImpl implements WorkItemService {

    @Autowired
    private WorkItemMapper workItemMapper;

    @Autowired
    private WorkItemStatusHistoryMapper workItemStatusHistoryMapper;

    @Autowired
    private ClarificationService clarificationService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CODE_PREFIX = "WI-";

    @Override
    @Transactional
    public WorkItemRespVO createWorkItem(WorkItemCreateReqDTO req) {
        WorkItemEntity entity = new WorkItemEntity();
        entity.setTitle(req.getTitle());
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

        entity.setStatus(WorkItemStatusEnum.DRAFT.getCode());

        String code = req.getCode();
        if (!StringUtils.hasText(code)) {
            // 并发场景下两个 createWorkItem 可能算出同一个 code，靠 uk_work_item_code 兜底重试
            boolean inserted = false;
            for (int attempt = 0; attempt < 3 && !inserted; attempt++) {
                code = generateCode();
                entity.setCode(code);
                try {
                    workItemMapper.insert(entity);
                    inserted = true;
                } catch (DuplicateKeyException e) {
                    if (attempt == 2) {
                        throw new BizException(ErrorCode.BIZ_VERSION_CONFLICT,
                            "无法生成唯一工作项编码，请稍后重试");
                    }
                }
            }
        } else {
            entity.setCode(code);
            try {
                workItemMapper.insert(entity);
            } catch (DuplicateKeyException e) {
                throw new BizException(ErrorCode.BIZ_DUPLICATE_CODE,
                    "工作项编码 " + code + " 已存在");
            }
        }

        WorkItemStatusHistoryEntity history = new WorkItemStatusHistoryEntity();
        history.setWorkItemId(entity.getId());
        history.setFromStatus(null);
        history.setToStatus(WorkItemStatusEnum.DRAFT.getCode());
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

        // 调真实计数
        vo.setP0OpenClarifications((int) Math.min(clarificationService.countP0Open(id), Integer.MAX_VALUE));
        vo.setTotalOpenClarifications((int) Math.min(clarificationService.countOpenByWorkItemId(id), Integer.MAX_VALUE));

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

        if (WorkItemStatusEnum.DONE.getCode().equals(entity.getStatus())) {
            throw new BizException(ErrorCode.BIZ_DONE_IMMUTABLE);
        }

        // @TableLogic 字段在 updateById 的 SET 子句中会被自动剔除，
        // 必须用 deleteById 才能正确写入 deleted=1。
        int rows = workItemMapper.deleteById(id);
        if (rows == 0) {
            throw new BizException(ErrorCode.BIZ_VERSION_CONFLICT);
        }
    }

    @Override
    public String getStatusCode(Long id) {
        WorkItemEntity entity = workItemMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return entity.getStatus();
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
        String prefix = CODE_PREFIX + date + "-";
        // 必须查全表（包含已软删除的记录），否则编码 2 已被占用却看不出，导致唯一键冲突
        String latestCode = workItemMapper.selectMaxCodeByPrefix(prefix);
        // 固定 %06d 序号宽度（单日最多 999,999 条），避免跨 10000 后字典序错乱
        long seq = 1L;
        if (latestCode != null && latestCode.length() > prefix.length()) {
            String tail = latestCode.substring(prefix.length());
            try {
                seq = Long.parseLong(tail) + 1;
            } catch (NumberFormatException ignored) {
                // fall back to seq=1 if existing code suffix is not numeric
            }
        }
        return prefix + String.format("%06d", seq);
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