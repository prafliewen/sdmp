package com.sdpm.workitem.vo;

public class WorkItemTransitionRespVO {

    private Long workItemId;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String transitionedAt;
    private Long historyId;

    public Long getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(Long workItemId) {
        this.workItemId = workItemId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getTransitionedAt() {
        return transitionedAt;
    }

    public void setTransitionedAt(String transitionedAt) {
        this.transitionedAt = transitionedAt;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }
}