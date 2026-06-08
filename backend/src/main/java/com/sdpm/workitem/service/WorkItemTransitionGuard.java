package com.sdpm.workitem.service;

import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;

public interface WorkItemTransitionGuard {

    void check(WorkItemEntity workItem, WorkItemStatusEnum target);
}