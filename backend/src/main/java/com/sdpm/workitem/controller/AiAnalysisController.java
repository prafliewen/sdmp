package com.sdpm.workitem.controller;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.dto.AiAnalysisTriggerReqDTO;
import com.sdpm.workitem.service.AiAnalysisService;
import com.sdpm.workitem.vo.AiAnalysisRespVO;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AiAnalysisController {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @PostMapping("/work-items/{id}/ai-analyses")
    public Result<AiAnalysisRespVO> triggerAnalysis(@PathVariable Long id,
                                                     @Valid @RequestBody AiAnalysisTriggerReqDTO req) {
        return Result.success(aiAnalysisService.triggerAnalysis(id, req.getAnalysisType()));
    }

    @GetMapping("/work-items/{id}/ai-analyses")
    public Result<PageResp<AiAnalysisRespVO>> listAnalyses(@PathVariable Long id,
                                                            @RequestParam(required = false) String analysisType,
                                                            @RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(aiAnalysisService.listAnalyses(id, analysisType, pageNo, pageSize));
    }
}