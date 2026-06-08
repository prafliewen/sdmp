package com.sdpm.workitem.service;

import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class StateMachine {

    private static final Map<WorkItemStatusEnum, Set<WorkItemStatusEnum>> TRANSITIONS = Map.of(
        WorkItemStatusEnum.DRAFT, Set.of(WorkItemStatusEnum.ANALYZING),
        WorkItemStatusEnum.ANALYZING, Set.of(WorkItemStatusEnum.READY, WorkItemStatusEnum.DRAFT),
        WorkItemStatusEnum.READY, Set.of(WorkItemStatusEnum.IN_PROGRESS, WorkItemStatusEnum.ANALYZING),
        WorkItemStatusEnum.IN_PROGRESS, Set.of(WorkItemStatusEnum.IN_TESTING, WorkItemStatusEnum.READY),
        WorkItemStatusEnum.IN_TESTING, Set.of(WorkItemStatusEnum.DONE, WorkItemStatusEnum.IN_PROGRESS),
        WorkItemStatusEnum.DONE, Set.of()
    );

    public boolean canTransit(WorkItemStatusEnum from, WorkItemStatusEnum to) {
        Set<WorkItemStatusEnum> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public void assertTransit(WorkItemStatusEnum from, WorkItemStatusEnum to) {
        if (from == WorkItemStatusEnum.DONE || to == WorkItemStatusEnum.DONE) {
            throw new BizException(ErrorCode.BIZ_DONE_IMMUTABLE);
        }
        if (!canTransit(from, to)) {
            throw new BizException(ErrorCode.BIZ_TRANSITION_NOT_ALLOWED,
                "不允许从 " + from.getDesc() + " 流转到 " + to.getDesc());
        }
    }

    public Set<WorkItemStatusEnum> getAllowedTransitions(WorkItemStatusEnum current) {
        return TRANSITIONS.getOrDefault(current, Set.of());
    }
}