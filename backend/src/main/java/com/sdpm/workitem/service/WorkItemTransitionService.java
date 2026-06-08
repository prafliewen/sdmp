package com.sdpm.workitem.service;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.vo.WorkItemStatusHistoryRespVO;
import com.sdpm.workitem.vo.WorkItemTransitionRespVO;

public interface WorkItemTransitionService {

    WorkItemTransitionRespVO transit(Long workItemId, String targetStatus, String reason);

    PageResp<WorkItemStatusHistoryRespVO> listHistory(Long workItemId, Integer pageNo, Integer pageSize);
}