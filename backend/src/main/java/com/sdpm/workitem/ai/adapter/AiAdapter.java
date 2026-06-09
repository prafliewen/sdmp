package com.sdpm.workitem.ai.adapter;

import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;
import com.sdpm.workitem.enumeration.AiSourceEnum;

import java.util.Map;

public interface AiAdapter {

    AiSourceEnum source();

    Map<String, Object> execute(AiAnalysisTypeEnum type, WorkItemEntity workItem);
}
