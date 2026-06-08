package com.sdpm.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.WorkItemCreateReqDTO;
import com.sdpm.workitem.dto.WorkItemUpdateReqDTO;
import com.sdpm.workitem.service.WorkItemService;
import com.sdpm.workitem.vo.WorkItemDetailRespVO;
import com.sdpm.workitem.vo.WorkItemRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.sdpm.workitem.controller.WorkItemController.class)
class WorkItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkItemService workItemService;

    private WorkItemRespVO respVO;

    @BeforeEach
    void setUp() {
        respVO = new WorkItemRespVO();
        respVO.setId(1L);
        respVO.setCode("WI-001");
        respVO.setTitle("测试工作项");
        respVO.setType("STORY");
        respVO.setPriority("P1");
        respVO.setStatus("DRAFT");
        respVO.setVersion(0L);
        respVO.setCreatedAt(LocalDateTime.now());
        respVO.setUpdatedAt(LocalDateTime.now());
    }

    // ========== 创建 ==========
    @Test
    @DisplayName("POST /api/v1/work-items → 创建工作项")
    void shouldCreateWorkItem() throws Exception {
        WorkItemCreateReqDTO req = new WorkItemCreateReqDTO();
        req.setTitle("新工作项");
        req.setType("STORY");

        when(workItemService.createWorkItem(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/v1/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("测试工作项"));

        verify(workItemService).createWorkItem(any());
    }

    @Test
    @DisplayName("POST /api/v1/work-items 无标题 → 400")
    void shouldRejectCreateWithoutTitle() throws Exception {
        WorkItemCreateReqDTO req = new WorkItemCreateReqDTO();
        req.setType("STORY");

        mockMvc.perform(post("/api/v1/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ========== 更新 ==========
    @Test
    @DisplayName("PUT /api/v1/work-items/{id} → 更新工作项")
    void shouldUpdateWorkItem() throws Exception {
        WorkItemUpdateReqDTO req = new WorkItemUpdateReqDTO();
        req.setTitle("更新标题");
        req.setVersion(0L);

        when(workItemService.updateWorkItem(eq(1L), any())).thenReturn(respVO);

        mockMvc.perform(put("/api/v1/work-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ========== 查询详情 ==========
    @Test
    @DisplayName("GET /api/v1/work-items/{id} → 查询详情")
    void shouldGetDetail() throws Exception {
        WorkItemDetailRespVO detail = new WorkItemDetailRespVO();
        detail.setId(1L);
        detail.setTitle("详情");
        detail.setStatus("DRAFT");

        when(workItemService.getWorkItemDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/work-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("详情"));
    }

    // ========== 分页查询 ==========
    @Test
    @DisplayName("GET /api/v1/work-items → 分页查询")
    void shouldPageWorkItems() throws Exception {
        PageResp<WorkItemRespVO> page = PageResp.of(1, 10, 1L,
                Collections.singletonList(respVO));
        when(workItemService.pageWorkItems(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/work-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("测试工作项"));
    }

    // ========== 删除 ==========
    @Test
    @DisplayName("DELETE /api/v1/work-items/{id} → 软删除")
    void shouldSoftDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/work-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workItemService).softDeleteWorkItem(1L);
    }
}