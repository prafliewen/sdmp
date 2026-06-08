package com.sdpm.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.AiAnalysisTriggerReqDTO;
import com.sdpm.workitem.service.AiAnalysisService;
import com.sdpm.workitem.vo.AiAnalysisRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.sdpm.workitem.controller.AiAnalysisController.class)
class AiAnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAnalysisService aiAnalysisService;

    private AiAnalysisRespVO respVO;

    @BeforeEach
    void setUp() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("headline", "需求摘要");

        respVO = new AiAnalysisRespVO();
        respVO.setId(1L);
        respVO.setWorkItemId(1L);
        respVO.setAnalysisType("SUMMARY");
        respVO.setSource("MOCK");
        respVO.setPayload(payload);
        respVO.setSummary("需求摘要");
        respVO.setCreatedAt("2026-06-08 10:00:00");
    }

    @Test
    @DisplayName("POST /api/v1/work-items/{id}/ai-analyses → 触发AI分析")
    void shouldTriggerAnalysis() throws Exception {
        AiAnalysisTriggerReqDTO req = new AiAnalysisTriggerReqDTO();
        req.setAnalysisType("SUMMARY");

        when(aiAnalysisService.triggerAnalysis(eq(1L), eq("SUMMARY"))).thenReturn(respVO);

        mockMvc.perform(post("/api/v1/work-items/1/ai-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.analysisType").value("SUMMARY"))
                .andExpect(jsonPath("$.data.source").value("MOCK"));

        verify(aiAnalysisService).triggerAnalysis(eq(1L), eq("SUMMARY"));
    }

    @Test
    @DisplayName("POST 无analysisType → 400")
    void shouldRejectTriggerWithoutType() throws Exception {
        AiAnalysisTriggerReqDTO req = new AiAnalysisTriggerReqDTO();

        mockMvc.perform(post("/api/v1/work-items/1/ai-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/work-items/{id}/ai-analyses → 查询分析记录")
    void shouldListAnalyses() throws Exception {
        PageResp<AiAnalysisRespVO> page = PageResp.of(1, 10, 1L,
                Collections.singletonList(respVO));
        when(aiAnalysisService.listAnalyses(eq(1L), eq(null), eq(1), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/work-items/1/ai-analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].analysisType").value("SUMMARY"));
    }
}