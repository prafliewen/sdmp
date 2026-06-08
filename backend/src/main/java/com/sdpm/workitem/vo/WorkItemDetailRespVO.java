package com.sdpm.workitem.vo;

import java.time.LocalDateTime;
import java.util.List;

public class WorkItemDetailRespVO {

    private Long id;
    private String code;
    private String title;
    private String description;
    private String type;
    private String priority;
    private String status;
    private String riskLevel;
    private String assignee;
    private String reporter;
    private List<String> tags;
    private List<String> acceptanceCriteria;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int p0OpenClarifications;
    private int totalOpenClarifications;
    private LocalDateTime lastTransitionTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getP0OpenClarifications() {
        return p0OpenClarifications;
    }

    public void setP0OpenClarifications(int p0OpenClarifications) {
        this.p0OpenClarifications = p0OpenClarifications;
    }

    public int getTotalOpenClarifications() {
        return totalOpenClarifications;
    }

    public void setTotalOpenClarifications(int totalOpenClarifications) {
        this.totalOpenClarifications = totalOpenClarifications;
    }

    public LocalDateTime getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(LocalDateTime lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }
}