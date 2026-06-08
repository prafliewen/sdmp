package com.sdpm.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.entity.ClarificationQuestionEntity;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.ClarificationSeverityEnum;
import com.sdpm.workitem.enumeration.ClarificationStatusEnum;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.ClarificationQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class P0ClarificationGuard implements WorkItemTransitionGuard {

    @Autowired
    private ClarificationQuestionMapper clarificationQuestionMapper;

    private static final Set<WorkItemStatusEnum> BLOCK_TARGETS = Set.of(
        WorkItemStatusEnum.READY,
        WorkItemStatusEnum.IN_PROGRESS,
        WorkItemStatusEnum.IN_TESTING,
        WorkItemStatusEnum.DONE
    );

    @Override
    public void check(WorkItemEntity workItem, WorkItemStatusEnum target) {
        if (!BLOCK_TARGETS.contains(target)) {
            return;
        }

        LambdaQueryWrapper<ClarificationQuestionEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ClarificationQuestionEntity::getWorkItemId, workItem.getId())
               .eq(ClarificationQuestionEntity::getSeverity, ClarificationSeverityEnum.P0.getCode())
               .eq(ClarificationQuestionEntity::getStatus, ClarificationStatusEnum.OPEN.getCode());
        long count = clarificationQuestionMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BizException(ErrorCode.BIZ_P0_CLARIFICATION_BLOCKED,
                "存在 " + count + " 条未解决的P0澄清问题，无法进入 " + target.getDesc());
        }
    }
}