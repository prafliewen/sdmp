package com.sdpm.workitem.dto;

import com.sdpm.workitem.enumeration.WorkItemPriorityEnum;
import com.sdpm.workitem.validation.EnumValue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class WorkItemUpdateReqDTO {

    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @Size(max = 10000, message = "描述长度不能超过10000个字符")
    private String description;

    @EnumValue(enumClass = WorkItemPriorityEnum.class, message = "优先级必须是 P0/P1/P2/P3 之一")
    private String priority;

    @Size(max = 64, message = "负责人长度不能超过64个字符")
    private String assignee;

    private List<String> tags;

    private List<String> acceptanceCriteria;

    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号不能小于0")
    private Long version;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(List<String> acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}