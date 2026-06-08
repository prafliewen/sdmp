package com.sdpm.workitem.dto;

import jakarta.validation.constraints.NotBlank;

public class AiAnalysisTriggerReqDTO {

    @NotBlank
    private String analysisType;

    private Boolean forceRefresh;

    public AiAnalysisTriggerReqDTO() {
        this.forceRefresh = false;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public Boolean getForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }
}