package com.sdpm.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.ClarificationCreateReqDTO;
import com.sdpm.workitem.dto.ClarificationResolveReqDTO;
import com.sdpm.workitem.service.ClarificationService;
import com.sdpm.workitem.vo.ClarificationRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.sdpm.workitem.controller.ClarificationController.class)
class ClarificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClarificationService clarificationService;

    private ClarificationRespVO respVO;

    @BeforeEach
    void setUp() {
        respVO = new ClarificationRespVO();
        respVO.setId(1L);
        respVO.setWorkItemId(1L);
        respVO.setQuestion("测试澄清问题");
        respVO.setSeverity("P0");
        respVO.setStatus("OPEN");
        respVO.setRaisedBy("张三");
        respVO.setCreatedAt("2026-06-08 10:00:00");
    }

    @Test
    @DisplayName("POST /api/v1/work-items/{id}/clarifications → 添加澄清问题")
    void shouldAddQuestion() throws Exception {
        ClarificationCreateReqDTO req = new ClarificationCreateReqDTO();
        req.setQuestion("测试问题");
        req.setSeverity("P0");

        when(clarificationService.addQuestion(eq(1L), any())).thenReturn(respVO);

        mockMvc.perform(post("/api/v1/work-items/1/clarifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.question").value("测试澄清问题"));

        verify(clarificationService).addQuestion(eq(1L), any());
    }

    @Test
    @DisplayName("POST /api/v1/work-items/{id}/clarifications 无question → 400")
    void shouldRejectAddWithoutQuestion() throws Exception {
        ClarificationCreateReqDTO req = new ClarificationCreateReqDTO();

        mockMvc.perform(post("/api/v1/work-items/1/clarifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/clarifications/{cid} → 解决澄清问题")
    void shouldResolveQuestion() throws Exception {
        ClarificationResolveReqDTO req = new ClarificationResolveReqDTO();
        req.setAnswer("已解决");
        respVO.setStatus("RESOLVED");
        respVO.setAnswer("已解决");

        when(clarificationService.resolveQuestion(eq(1L), any())).thenReturn(respVO);

        mockMvc.perform(put("/api/v1/clarifications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("GET /api/v1/work-items/{id}/clarifications → 查询澄清列表")
    void shouldListQuestions() throws Exception {
        PageResp<ClarificationRespVO> page = PageResp.of(1, 20, 1L,
                Collections.singletonList(respVO));
        when(clarificationService.listQuestions(eq(1L), any(), any(), eq(1), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/work-items/1/clarifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}