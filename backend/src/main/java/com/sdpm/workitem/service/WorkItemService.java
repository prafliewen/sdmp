package com.sdpm.workitem.service;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.WorkItemCreateReqDTO;
import com.sdpm.workitem.dto.WorkItemQueryReqDTO;
import com.sdpm.workitem.dto.WorkItemUpdateReqDTO;
import com.sdpm.workitem.vo.WorkItemDetailRespVO;
import com.sdpm.workitem.vo.WorkItemRespVO;

public interface WorkItemService {

    WorkItemRespVO createWorkItem(WorkItemCreateReqDTO req);

    WorkItemRespVO updateWorkItem(Long id, WorkItemUpdateReqDTO req);

    WorkItemDetailRespVO getWorkItemDetail(Long id);

    PageResp<WorkItemRespVO> pageWorkItems(WorkItemQueryReqDTO req);

    void softDeleteWorkItem(Long id);

    String getStatusCode(Long id);
}