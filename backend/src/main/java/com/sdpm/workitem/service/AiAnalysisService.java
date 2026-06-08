package com.sdpm.workitem.service;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.vo.AiAnalysisRespVO;

public interface AiAnalysisService {

    AiAnalysisRespVO triggerAnalysis(Long workItemId, String analysisType);

    PageResp<AiAnalysisRespVO> listAnalyses(Long workItemId, String analysisType, Integer pageNo, Integer pageSize);
}