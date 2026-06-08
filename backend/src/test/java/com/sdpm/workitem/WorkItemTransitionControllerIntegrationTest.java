package com.sdpm.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.WorkItemTransitionReqDTO;
import com.sdpm.workitem.service.WorkItemTransitionService;
import com.sdpm.workitem.vo.WorkItemStatusHistoryRespVO;
import com.sdpm.workitem.vo.WorkItemTransitionRespVO;
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

@WebMvcTest(com.sdpm.workitem.controller.WorkItemTransitionController.class)
class WorkItemTransitionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkItemTransitionService transitionService;

    private WorkItemTransitionRespVO respVO;

    @BeforeEach
    void setUp() {
        respVO = new WorkItemTransitionRespVO();
        respVO.setWorkItemId(1L);
        respVO.setFromStatus("DRAFT");
        respVO.setToStatus("ANALYZING");
        respVO.setOperator("张三");
        respVO.setTransitionedAt("2026-06-08T10:00:00");
        respVO.setHistoryId(1L);
    }

    @Test
    @DisplayName("POST /api/v1/work-items/{id}/transitions → 状态流转")
    void shouldTransit() throws Exception {
        WorkItemTransitionReqDTO req = new WorkItemTransitionReqDTO();
        req.setTargetStatus("ANALYZING");
        req.setReason("开始分析");

        when(transitionService.transit(eq(1L), eq("ANALYZING"), eq("开始分析"))).thenReturn(respVO);

        mockMvc.perform(post("/api/v1/work-items/1/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fromStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.toStatus").value("ANALYZING"));

        verify(transitionService).transit(eq(1L), eq("ANALYZING"), eq("开始分析"));
    }

    @Test
    @DisplayName("POST 流转无targetStatus → 400")
    void shouldRejectTransitWithoutTarget() throws Exception {
        WorkItemTransitionReqDTO req = new WorkItemTransitionReqDTO();

        mockMvc.perform(post("/api/v1/work-items/1/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/work-items/{id}/transitions → 查询流转历史")
    void shouldListHistory() throws Exception {
        WorkItemStatusHistoryRespVO history = new WorkItemStatusHistoryRespVO();
        history.setId(1L);
        history.setWorkItemId(1L);
        history.setFromStatus("DRAFT");
        history.setToStatus("ANALYZING");
        history.setReason("开始分析");
        history.setOperator("张三");

        PageResp<WorkItemStatusHistoryRespVO> page = PageResp.of(1, 20, 1L,
                Collections.singletonList(history));
        when(transitionService.listHistory(eq(1L), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/v1/work-items/1/transitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].fromStatus").value("DRAFT"));
    }
}