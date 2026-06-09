package com.sdpm.workitem.dto;

import com.sdpm.workitem.enumeration.WorkItemPriorityEnum;
import com.sdpm.workitem.enumeration.WorkItemTypeEnum;
import com.sdpm.workitem.validation.EnumValue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class WorkItemCreateReqDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @Size(max = 64, message = "编号长度不能超过64个字符")
    private String code;

    @Size(max = 10000, message = "描述长度不能超过10000个字符")
    private String description;

    @NotBlank(message = "类型不能为空")
    @EnumValue(enumClass = WorkItemTypeEnum.class, message = "类型必须是 STORY/BUG/TASK 之一")
    private String type;

    @EnumValue(enumClass = WorkItemPriorityEnum.class, message = "优先级必须是 P0/P1/P2/P3 之一")
    private String priority = "P2";

    @Size(max = 64, message = "负责人长度不能超过64个字符")
    private String assignee;

    @Size(max = 64, message = "报告人长度不能超过64个字符")
    private String reporter;

    private List<String> tags;

    private List<String> acceptanceCriteria;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
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
}