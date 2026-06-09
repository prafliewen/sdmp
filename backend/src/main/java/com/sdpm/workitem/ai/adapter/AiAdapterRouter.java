package com.sdpm.workitem.ai.adapter;

import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.AiAnalysisTypeEnum;
import com.sdpm.workitem.enumeration.AiSourceEnum;
import com.sdpm.workitem.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiAdapterRouter {

    private final Map<AiSourceEnum, AiAdapter> adapters;
    private final AiSourceEnum activeSource;

    public AiAdapterRouter(List<AiAdapter> adapterList,
                           @Value("${ai.source:mock}") String source) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(AiAdapter::source, a -> a, (a, b) -> a));
        AiSourceEnum parsed;
        try {
            parsed = AiSourceEnum.fromCode(source);
        } catch (IllegalArgumentException e) {
            parsed = AiSourceEnum.MOCK;
        }
        this.activeSource = parsed;
    }

    public AiSourceEnum activeSource() {
        return activeSource;
    }

    public Map<String, Object> execute(AiAnalysisTypeEnum type, WorkItemEntity workItem) {
        AiAdapter adapter = adapters.get(activeSource);
        if (adapter == null) {
            throw new BizException(ErrorCode.BIZ_AI_UPSTREAM_FAILURE,
                "未找到 ai.source=" + activeSource.getCode() + " 对应的 AI 适配器");
        }
        return adapter.execute(type, workItem);
    }
}
