package com.sdpm.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkItemTransitionReqDTO {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @Size(max = 500, message = "流转原因长度不能超过500个字符")
    private String reason;

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}