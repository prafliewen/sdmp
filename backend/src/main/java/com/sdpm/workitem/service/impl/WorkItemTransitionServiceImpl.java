package com.sdpm.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.config.UserContext;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.entity.WorkItemStatusHistoryEntity;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.mapper.WorkItemStatusHistoryMapper;
import com.sdpm.workitem.service.StateMachine;
import com.sdpm.workitem.service.WorkItemTransitionGuard;
import com.sdpm.workitem.service.WorkItemTransitionService;
import com.sdpm.workitem.vo.WorkItemStatusHistoryRespVO;
import com.sdpm.workitem.vo.WorkItemTransitionRespVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class WorkItemTransitionServiceImpl implements WorkItemTransitionService {

    @Autowired
    private WorkItemMapper workItemMapper;

    @Autowired
    private WorkItemStatusHistoryMapper historyMapper;

    @Autowired
    private StateMachine stateMachine;

    @Autowired(required = false)
    private List<WorkItemTransitionGuard> guards;

    @Override
    @Transactional
    public WorkItemTransitionRespVO transit(Long workItemId, String targetStatus, String reason) {
        WorkItemEntity workItem = workItemMapper.selectById(workItemId);
        if (workItem == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        WorkItemStatusEnum current = WorkItemStatusEnum.fromCode(workItem.getStatus());
        WorkItemStatusEnum target = WorkItemStatusEnum.fromCode(targetStatus);

        stateMachine.assertTransit(current, target);

        if (guards != null) {
            for (WorkItemTransitionGuard guard : guards) {
                guard.check(workItem, target);
            }
        }

        workItem.setStatus(target.getCode());
        workItemMapper.updateById(workItem);

        String operator = UserContext.getOperator();
        WorkItemStatusHistoryEntity history = new WorkItemStatusHistoryEntity();
        history.setWorkItemId(workItemId);
        history.setFromStatus(current.getCode());
        history.setToStatus(target.getCode());
        history.setReason(reason);
        history.setOperator(operator);
        historyMapper.insert(history);

        WorkItemTransitionRespVO vo = new WorkItemTransitionRespVO();
        vo.setWorkItemId(workItemId);
        vo.setFromStatus(current.getCode());
        vo.setToStatus(target.getCode());
        vo.setOperator(operator);
        vo.setTransitionedAt(LocalDateTime.now().toString());
        vo.setHistoryId(history.getId());
        return vo;
    }

    @Override
    public PageResp<WorkItemStatusHistoryRespVO> listHistory(Long workItemId, Integer pageNo, Integer pageSize) {
        Page<WorkItemStatusHistoryEntity> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<WorkItemStatusHistoryEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WorkItemStatusHistoryEntity::getWorkItemId, workItemId)
               .orderByDesc(WorkItemStatusHistoryEntity::getCreatedAt);
        Page<WorkItemStatusHistoryEntity> result = historyMapper.selectPage(page, wrapper);

        List<WorkItemStatusHistoryEntity> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResp.of(pageNo, pageSize, result.getTotal(), Collections.emptyList());
        }

        List<WorkItemStatusHistoryRespVO> voList = records.stream().map(h -> {
            WorkItemStatusHistoryRespVO vo = new WorkItemStatusHistoryRespVO();
            vo.setId(h.getId());
            vo.setWorkItemId(h.getWorkItemId());
            vo.setFromStatus(h.getFromStatus());
            vo.setToStatus(h.getToStatus());
            vo.setReason(h.getReason());
            vo.setOperator(h.getOperator());
            vo.setCreatedAt(h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
            return vo;
        }).toList();

        return PageResp.of(pageNo, pageSize, result.getTotal(), voList);
    }
}