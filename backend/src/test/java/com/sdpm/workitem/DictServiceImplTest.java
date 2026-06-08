package com.sdpm.workitem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.dto.DictCreateReqDTO;
import com.sdpm.workitem.dto.DictUpdateReqDTO;
import com.sdpm.workitem.entity.DictItemEntity;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.DictItemMapper;
import com.sdpm.workitem.service.impl.DictServiceImpl;
import com.sdpm.workitem.vo.DictRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictServiceImplTest {

    @Mock
    private DictItemMapper dictItemMapper;

    @InjectMocks
    private DictServiceImpl dictService;

    private DictItemEntity dictEntity;

    @BeforeEach
    void setUp() {
        dictEntity = new DictItemEntity();
        dictEntity.setId(1L);
        dictEntity.setType("PRIORITY");
        dictEntity.setKey("P0");
        dictEntity.setLabel("紧急");
        dictEntity.setValue("0");
        dictEntity.setSort(1);
        dictEntity.setEnabled(1);
    }

    // ========== 列表查询 ==========
    @Test
    @DisplayName("按类型查询字典列表")
    void shouldListByType() {
        when(dictItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(dictEntity));

        List<DictRespVO> result = dictService.listByType("PRIORITY", true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P0", result.get(0).getKey());
    }

    @Test
    @DisplayName("enabledOnly=false 时返回全部")
    void shouldListAllWhenDisabledFilter() {
        DictItemEntity disabled = new DictItemEntity();
        disabled.setId(2L);
        disabled.setType("PRIORITY");
        disabled.setKey("P1");
        disabled.setEnabled(0);
        when(dictItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(dictEntity, disabled));

        List<DictRespVO> result = dictService.listByType("PRIORITY", false);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("空结果返回空列表")
    void shouldReturnEmptyList() {
        when(dictItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<DictRespVO> result = dictService.listByType("NONEXIST", true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== 创建 ==========
    @Test
    @DisplayName("创建字典成功")
    void shouldCreateDict() {
        DictCreateReqDTO dto = new DictCreateReqDTO();
        dto.setType("PRIORITY");
        dto.setKey("P0");
        dto.setLabel("紧急");
        dto.setValue("0");

        when(dictItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dictItemMapper.insert(any(DictItemEntity.class))).thenReturn(1);

        DictRespVO result = dictService.createDict(dto);

        assertNotNull(result);
        assertEquals("P0", result.getKey());
        assertEquals("紧急", result.getLabel());
        assertTrue(result.getEnabled());
    }

    @Test
    @DisplayName("创建重复type+key → 抛BIZ_DUPLICATE_DICT_KEY")
    void shouldRejectDuplicateDictKey() {
        DictCreateReqDTO dto = new DictCreateReqDTO();
        dto.setType("PRIORITY");
        dto.setKey("P0");
        dto.setLabel("紧急");

        when(dictItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
                () -> dictService.createDict(dto));
        assertEquals(ErrorCode.BIZ_DUPLICATE_DICT_KEY.getCode(), ex.getErrorCode().getCode());
    }

    // ========== 更新 ==========
    @Test
    @DisplayName("更新字典成功")
    void shouldUpdateDict() {
        DictUpdateReqDTO dto = new DictUpdateReqDTO();
        dto.setLabel("更新后的标签");

        when(dictItemMapper.selectById(1L)).thenReturn(dictEntity);
        when(dictItemMapper.updateById(any(DictItemEntity.class))).thenReturn(1);

        DictRespVO result = dictService.updateDict(1L, dto);

        assertEquals("更新后的标签", result.getLabel());
    }

    @Test
    @DisplayName("更新不存在的字典 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnUpdate() {
        DictUpdateReqDTO dto = new DictUpdateReqDTO();
        dto.setLabel("新标签");
        when(dictItemMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> dictService.updateDict(999L, dto));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    // ========== 删除 ==========
    @Test
    @DisplayName("删除字典成功")
    void shouldDeleteDict() {
        when(dictItemMapper.selectById(1L)).thenReturn(dictEntity);
        when(dictItemMapper.deleteById(1L)).thenReturn(1);

        boolean result = dictService.deleteDict(1L);

        assertTrue(result);
        verify(dictItemMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除不存在的字典 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnDelete() {
        when(dictItemMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> dictService.deleteDict(999L));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }
}