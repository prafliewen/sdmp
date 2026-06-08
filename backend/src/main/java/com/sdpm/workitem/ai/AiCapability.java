package com.sdpm.workitem.ai;

import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;

import java.util.Map;

public interface AiCapability {

    AiAnalysisTypeEnum supports();

    Map<String, Object> analyse(WorkItemEntity workItem);
}