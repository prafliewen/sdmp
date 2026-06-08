package com.sdpm.workitem.service;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.ClarificationCreateReqDTO;
import com.sdpm.workitem.dto.ClarificationResolveReqDTO;
import com.sdpm.workitem.vo.ClarificationRespVO;

public interface ClarificationService {

    ClarificationRespVO addQuestion(Long workItemId, ClarificationCreateReqDTO dto);

    ClarificationRespVO resolveQuestion(Long questionId, ClarificationResolveReqDTO dto);

    PageResp<ClarificationRespVO> listQuestions(Long workItemId, String severity, String status, Integer pageNo, Integer pageSize);

    long countP0Open(Long workItemId);
}