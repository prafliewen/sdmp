package com.sdpm.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdpm.workitem.dto.DictCreateReqDTO;
import com.sdpm.workitem.dto.DictUpdateReqDTO;
import com.sdpm.workitem.service.DictService;
import com.sdpm.workitem.vo.DictRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.sdpm.workitem.controller.DictController.class)
class DictControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DictService dictService;

    private DictRespVO respVO;

    @BeforeEach
    void setUp() {
        respVO = new DictRespVO();
        respVO.setId(1L);
        respVO.setType("PRIORITY");
        respVO.setKey("P0");
        respVO.setLabel("紧急");
        respVO.setValue("0");
        respVO.setSort(1);
        respVO.setEnabled(true);
    }

    @Test
    @DisplayName("GET /api/v1/dicts?type=PRIORITY → 查询字典列表")
    void shouldListDicts() throws Exception {
        when(dictService.listByType(eq("PRIORITY"), eq(true)))
                .thenReturn(Arrays.asList(respVO));

        mockMvc.perform(get("/api/v1/dicts")
                        .param("type", "PRIORITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].key").value("P0"));
    }

    @Test
    @DisplayName("GET /api/v1/dicts?type=PRIORITY&enabledOnly=false → 查询全部")
    void shouldListAllDicts() throws Exception {
        when(dictService.listByType(eq("PRIORITY"), eq(false)))
                .thenReturn(Arrays.asList(respVO));

        mockMvc.perform(get("/api/v1/dicts")
                        .param("type", "PRIORITY")
                        .param("enabledOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/dicts → 创建字典")
    void shouldCreateDict() throws Exception {
        DictCreateReqDTO req = new DictCreateReqDTO();
        req.setType("PRIORITY");
        req.setKey("P0");
        req.setLabel("紧急");

        when(dictService.createDict(any())).thenReturn(respVO);

        mockMvc.perform(post("/api/v1/dicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.key").value("P0"));

        verify(dictService).createDict(any());
    }

    @Test
    @DisplayName("POST /api/v1/dicts 无type → 400")
    void shouldRejectCreateWithoutType() throws Exception {
        DictCreateReqDTO req = new DictCreateReqDTO();
        req.setKey("P0");
        req.setLabel("紧急");

        mockMvc.perform(post("/api/v1/dicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/dicts/{id} → 更新字典")
    void shouldUpdateDict() throws Exception {
        DictUpdateReqDTO req = new DictUpdateReqDTO();
        req.setLabel("更新标签");

        when(dictService.updateDict(eq(1L), any())).thenReturn(respVO);

        mockMvc.perform(put("/api/v1/dicts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/dicts/{id} → 删除字典")
    void shouldDeleteDict() throws Exception {
        when(dictService.deleteDict(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/dicts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(dictService).deleteDict(1L);
    }
}