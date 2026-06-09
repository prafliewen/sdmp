package com.sdpm.workitem.dto;

import com.sdpm.workitem.enumeration.ClarificationSeverityEnum;
import com.sdpm.workitem.validation.EnumValue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClarificationCreateReqDTO {

    @NotBlank
    @Size(max = 2000)
    private String question;

    @EnumValue(enumClass = ClarificationSeverityEnum.class, message = "严重度必须是 P0/P1/P2 之一")
    private String severity;

    @Size(max = 64)
    private String raisedBy;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(String raisedBy) {
        this.raisedBy = raisedBy;
    }
}