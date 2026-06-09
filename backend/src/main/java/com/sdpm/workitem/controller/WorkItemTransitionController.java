package com.sdpm.workitem.controller;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.dto.WorkItemTransitionReqDTO;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.service.StateMachine;
import com.sdpm.workitem.service.WorkItemService;
import com.sdpm.workitem.service.WorkItemTransitionService;
import com.sdpm.workitem.vo.WorkItemStatusHistoryRespVO;
import com.sdpm.workitem.vo.WorkItemTransitionRespVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/v1")
public class WorkItemTransitionController {

    @Autowired
    private WorkItemTransitionService transitionService;

    @Autowired
    private WorkItemService workItemService;

    @Autowired
    private StateMachine stateMachine;

    @PostMapping("/work-items/{id}/transitions")
    public Result<WorkItemTransitionRespVO> transit(
            @PathVariable Long id,
            @Valid @RequestBody WorkItemTransitionReqDTO req) {
        return Result.success(transitionService.transit(id, req.getTargetStatus(), req.getReason()));
    }

    @GetMapping("/work-items/{id}/transitions")
    public Result<PageResp<WorkItemStatusHistoryRespVO>> listHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer pageNo,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer pageSize) {
        return Result.success(transitionService.listHistory(id, pageNo, pageSize));
    }

    @GetMapping("/work-items/{id}/transitions/allowed")
    public Result<List<String>> listAllowedTransitions(@PathVariable Long id) {
        String currentStatus = workItemService.getStatusCode(id);
        if (currentStatus == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }
        WorkItemStatusEnum current;
        try {
            current = WorkItemStatusEnum.fromCode(currentStatus);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }
        Set<WorkItemStatusEnum> allowed = stateMachine.getAllowedTransitions(current);
        List<String> codes = allowed.stream()
                .map(WorkItemStatusEnum::getCode)
                .collect(Collectors.toList());
        return Result.success(codes);
    }
}