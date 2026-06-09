package com.sdpm.workitem.dto;

import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.validation.EnumValue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkItemTransitionReqDTO {

    @NotBlank(message = "目标状态不能为空")
    @EnumValue(enumClass = WorkItemStatusEnum.class, message = "目标状态必须是合法的状态枚举值")
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