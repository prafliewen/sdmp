package com.sdpm.workitem.ai.capability;

import com.sdpm.workitem.ai.AiCapability;
import com.sdpm.workitem.ai.adapter.AiAdapterRouter;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SummaryCapability implements AiCapability {

    @Autowired
    private AiAdapterRouter aiAdapterRouter;

    @Override
    public AiAnalysisTypeEnum supports() {
        return AiAnalysisTypeEnum.SUMMARY;
    }

    @Override
    public Map<String, Object> analyse(WorkItemEntity workItem) {
        return aiAdapterRouter.execute(AiAnalysisTypeEnum.SUMMARY, workItem);
    }
}
