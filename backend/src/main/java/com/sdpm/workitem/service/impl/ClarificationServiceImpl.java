package com.sdpm.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.config.UserContext;
import com.sdpm.workitem.dto.ClarificationCreateReqDTO;
import com.sdpm.workitem.dto.ClarificationResolveReqDTO;
import com.sdpm.workitem.entity.ClarificationQuestionEntity;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.ClarificationStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.ClarificationQuestionMapper;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.service.ClarificationService;
import com.sdpm.workitem.vo.ClarificationRespVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ClarificationServiceImpl implements ClarificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ClarificationQuestionMapper clarificationQuestionMapper;

    @Autowired
    private WorkItemMapper workItemMapper;

    @Override
    @Transactional
    public ClarificationRespVO addQuestion(Long workItemId, ClarificationCreateReqDTO dto) {
        WorkItemEntity workItem = workItemMapper.selectById(workItemId);
        if (workItem == null || (workItem.getDeleted() != null && workItem.getDeleted() != 0)) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        ClarificationQuestionEntity entity = new ClarificationQuestionEntity();
        entity.setWorkItemId(workItemId);
        entity.setQuestion(dto.getQuestion());
        entity.setSeverity(StringUtils.hasText(dto.getSeverity()) ? dto.getSeverity() : "P1");
        entity.setStatus(ClarificationStatusEnum.OPEN.getCode());
        entity.setRaisedBy(StringUtils.hasText(dto.getRaisedBy()) ? dto.getRaisedBy() : UserContext.getOperator());
        entity.setCreatedAt(LocalDateTime.now());

        try {
            clarificationQuestionMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // uk_clarification_question_work_item_question 兜底
            throw new BizException(ErrorCode.BIZ_DUPLICATE_QUESTION);
        }

        return toRespVO(entity);
    }

    @Override
    @Transactional
    public ClarificationRespVO resolveQuestion(Long questionId, ClarificationResolveReqDTO dto) {
        ClarificationQuestionEntity entity = clarificationQuestionMapper.selectById(questionId);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        if (ClarificationStatusEnum.RESOLVED.getCode().equals(entity.getStatus())) {
            throw new BizException(ErrorCode.BIZ_CLARIFICATION_ALREADY_RESOLVED);
        }

        entity.setAnswer(dto.getAnswer());
        entity.setResolvedBy(StringUtils.hasText(dto.getResolvedBy()) ? dto.getResolvedBy() : UserContext.getOperator());
        entity.setResolvedAt(LocalDateTime.now());
        entity.setStatus(ClarificationStatusEnum.RESOLVED.getCode());

        int rows = clarificationQuestionMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.BIZ_VERSION_CONFLICT);
        }

        return toRespVO(entity);
    }

    @Override
    public PageResp<ClarificationRespVO> listQuestions(Long workItemId, String severity, String status, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<ClarificationQuestionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClarificationQuestionEntity::getWorkItemId, workItemId);

        if (StringUtils.hasText(severity)) {
            wrapper.eq(ClarificationQuestionEntity::getSeverity, severity);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ClarificationQuestionEntity::getStatus, status);
        }

        wrapper.orderByDesc(ClarificationQuestionEntity::getCreatedAt);

        Page<ClarificationQuestionEntity> page = new Page<>(pageNo, pageSize);
        IPage<ClarificationQuestionEntity> result = clarificationQuestionMapper.selectPage(page, wrapper);

        List<ClarificationRespVO> records = result.getRecords().stream()
                .map(this::toRespVO)
                .toList();

        return PageResp.of(pageNo, pageSize, result.getTotal(), records);
    }

    @Override
    public long countP0Open(Long workItemId) {
        LambdaQueryWrapper<ClarificationQuestionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClarificationQuestionEntity::getWorkItemId, workItemId)
                .eq(ClarificationQuestionEntity::getSeverity, "P0")
                .eq(ClarificationQuestionEntity::getStatus, ClarificationStatusEnum.OPEN.getCode());

        return clarificationQuestionMapper.selectCount(wrapper);
    }

    @Override
    public long countOpenByWorkItemId(Long workItemId) {
        LambdaQueryWrapper<ClarificationQuestionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClarificationQuestionEntity::getWorkItemId, workItemId)
                .eq(ClarificationQuestionEntity::getStatus, ClarificationStatusEnum.OPEN.getCode());
        return clarificationQuestionMapper.selectCount(wrapper);
    }

    private ClarificationRespVO toRespVO(ClarificationQuestionEntity entity) {
        ClarificationRespVO vo = new ClarificationRespVO();
        vo.setId(entity.getId());
        vo.setWorkItemId(entity.getWorkItemId());
        vo.setQuestion(entity.getQuestion());
        vo.setSeverity(entity.getSeverity());
        vo.setStatus(entity.getStatus());
        vo.setAnswer(entity.getAnswer());
        vo.setRaisedBy(entity.getRaisedBy());
        vo.setResolvedBy(entity.getResolvedBy());
        vo.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(DATE_TIME_FORMATTER) : null);
        vo.setResolvedAt(entity.getResolvedAt() != null ? entity.getResolvedAt().format(DATE_TIME_FORMATTER) : null);
        return vo;
    }
}