package com.sdpm.workitem.ai.capability;

import com.sdpm.workitem.ai.AiCapability;
import com.sdpm.workitem.ai.adapter.AiAdapter;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ClarificationCapability implements AiCapability {

    @Autowired
    private AiAdapter aiAdapter;

    @Override
    public AiAnalysisTypeEnum supports() {
        return AiAnalysisTypeEnum.CLARIFICATION;
    }

    @Override
    public Map<String, Object> analyse(WorkItemEntity workItem) {
        return aiAdapter.execute(AiAnalysisTypeEnum.CLARIFICATION, workItem);
    }
}