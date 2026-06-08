package com.sdpm.workitem.controller;

import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.dto.WorkItemCreateReqDTO;
import com.sdpm.workitem.dto.WorkItemQueryReqDTO;
import com.sdpm.workitem.dto.WorkItemUpdateReqDTO;
import com.sdpm.workitem.service.WorkItemService;
import com.sdpm.workitem.vo.WorkItemDetailRespVO;
import com.sdpm.workitem.vo.WorkItemRespVO;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-items")
public class WorkItemController {

    @Autowired
    private WorkItemService workItemService;

    @PostMapping
    public Result<WorkItemRespVO> createWorkItem(@Valid @RequestBody WorkItemCreateReqDTO req) {
        return Result.success(workItemService.createWorkItem(req));
    }

    @PutMapping("/{id}")
    public Result<WorkItemRespVO> updateWorkItem(@PathVariable Long id, @Valid @RequestBody WorkItemUpdateReqDTO req) {
        return Result.success(workItemService.updateWorkItem(id, req));
    }

    @GetMapping("/{id}")
    public Result<WorkItemDetailRespVO> getWorkItemDetail(@PathVariable Long id) {
        return Result.success(workItemService.getWorkItemDetail(id));
    }

    @GetMapping
    public Result<PageResp<WorkItemRespVO>> pageWorkItems(@Valid WorkItemQueryReqDTO req) {
        return Result.success(workItemService.pageWorkItems(req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteWorkItem(@PathVariable Long id) {
        workItemService.softDeleteWorkItem(id);
        return Result.success(true);
    }
}