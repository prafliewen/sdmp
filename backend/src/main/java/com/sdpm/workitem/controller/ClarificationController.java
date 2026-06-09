package com.sdpm.workitem.controller;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.dto.ClarificationCreateReqDTO;
import com.sdpm.workitem.dto.ClarificationResolveReqDTO;
import com.sdpm.workitem.service.ClarificationService;
import com.sdpm.workitem.vo.ClarificationRespVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ClarificationController {

    @Autowired
    private ClarificationService clarificationService;

    @PostMapping("/work-items/{id}/clarifications")
    public Result<ClarificationRespVO> addQuestion(@PathVariable Long id, @Valid @RequestBody ClarificationCreateReqDTO dto) {
        return Result.success(clarificationService.addQuestion(id, dto));
    }

    @PutMapping("/clarifications/{cid}")
    public Result<ClarificationRespVO> resolveQuestion(@PathVariable Long cid, @Valid @RequestBody ClarificationResolveReqDTO dto) {
        return Result.success(clarificationService.resolveQuestion(cid, dto));
    }

    @GetMapping("/work-items/{id}/clarifications")
    public Result<PageResp<ClarificationRespVO>> listQuestions(@PathVariable Long id,
                                                                @RequestParam(required = false) String severity,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(defaultValue = "1")
                                                                @Min(value = 1, message = "页码不能小于1") Integer pageNo,
                                                                @RequestParam(defaultValue = "20")
                                                                @Min(value = 1, message = "每页条数不能小于1")
                                                                @Max(value = 100, message = "每页条数不能超过100") Integer pageSize) {
        return Result.success(clarificationService.listQuestions(id, severity, status, pageNo, pageSize));
    }
}