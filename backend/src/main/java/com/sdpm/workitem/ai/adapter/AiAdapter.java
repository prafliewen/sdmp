package com.sdpm.workitem.ai.adapter;

import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;

import java.util.Map;

public interface AiAdapter {

    Map<String, Object> execute(AiAnalysisTypeEnum type, WorkItemEntity workItem);
}