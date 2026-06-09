package com.sdpm.workitem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.entity.WorkItemStatusHistoryEntity;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.mapper.WorkItemStatusHistoryMapper;
import com.sdpm.workitem.service.StateMachine;
import com.sdpm.workitem.service.WorkItemTransitionGuard;
import com.sdpm.workitem.service.impl.WorkItemTransitionServiceImpl;
import com.sdpm.workitem.vo.WorkItemStatusHistoryRespVO;
import com.sdpm.workitem.vo.WorkItemTransitionRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkItemTransitionServiceImplTest {

    @Mock
    private WorkItemMapper workItemMapper;

    @Mock
    private WorkItemStatusHistoryMapper historyMapper;

    @Mock
    private StateMachine stateMachine;

    @InjectMocks
    private WorkItemTransitionServiceImpl transitionService;

    private WorkItemEntity workItem;

    @BeforeEach
    void setUp() {
        workItem = new WorkItemEntity();
        workItem.setId(1L);
        workItem.setTitle("测试工作项");
        workItem.setStatus("DRAFT");
        workItem.setVersion(0L);
    }

    // ========== 状态流转 ==========
    @Test
    @DisplayName("正常流转 DRAFT → ANALYZING")
    void shouldTransitSuccessfully() {
        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(workItemMapper.updateById(any(WorkItemEntity.class))).thenReturn(1);
        when(historyMapper.insert(any(WorkItemStatusHistoryEntity.class))).thenReturn(1);

        WorkItemTransitionRespVO result = transitionService.transit(1L, "ANALYZING", "开始分析");

        assertNotNull(result);
        assertEquals("DRAFT", result.getFromStatus());
        assertEquals("ANALYZING", result.getToStatus());
        verify(stateMachine).assertTransit(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.ANALYZING);
    }

    @Test
    @DisplayName("流转不存在的工作项 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnTransit() {
        when(workItemMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> transitionService.transit(999L, "ANALYZING", "原因"));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("乐观锁冲突 → 抛BIZ_VERSION_CONFLICT")
    void shouldThrowVersionConflictOnTransit() {
        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(workItemMapper.updateById(any(WorkItemEntity.class))).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> transitionService.transit(1L, "ANALYZING", "原因"));
        assertEquals(ErrorCode.BIZ_VERSION_CONFLICT.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("非法流转被StateMachine拦截")
    void shouldBeBlockedByStateMachine() {
        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        doThrow(new BizException(ErrorCode.BIZ_TRANSITION_NOT_ALLOWED))
                .when(stateMachine).assertTransit(any(), any());

        BizException ex = assertThrows(BizException.class,
                () -> transitionService.transit(1L, "DONE", "直接完成"));
        assertEquals(ErrorCode.BIZ_TRANSITION_NOT_ALLOWED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Guard拦截P0未解决时的流转")
    void shouldBeBlockedByGuard() throws Exception {
        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        // 通过反射注入一个抛异常的 mock guard
        WorkItemTransitionGuard blockingGuard = mock(WorkItemTransitionGuard.class);
        doThrow(new BizException(ErrorCode.BIZ_P0_CLARIFICATION_BLOCKED, "P0未解决"))
                .when(blockingGuard).check(any(WorkItemEntity.class), any(WorkItemStatusEnum.class));
        java.lang.reflect.Field guardsField = WorkItemTransitionServiceImpl.class.getDeclaredField("guards");
        guardsField.setAccessible(true);
        guardsField.set(transitionService, Collections.singletonList(blockingGuard));

        BizException ex = assertThrows(BizException.class,
                () -> transitionService.transit(1L, "ANALYZING", "测试"));
        assertEquals(ErrorCode.BIZ_P0_CLARIFICATION_BLOCKED.getCode(), ex.getErrorCode().getCode());
        verify(blockingGuard).check(any(WorkItemEntity.class), eq(WorkItemStatusEnum.ANALYZING));
        // 拦截后不应执行 updateById / historyMapper.insert
        verify(workItemMapper, never()).updateById(any(WorkItemEntity.class));
        verify(historyMapper, never()).insert(any(WorkItemStatusHistoryEntity.class));
    }

    // ========== 查询历史 ==========
    @Test
    @DisplayName("查询流转历史")
    void shouldListHistory() {
        WorkItemStatusHistoryEntity history = new WorkItemStatusHistoryEntity();
        history.setId(1L);
        history.setWorkItemId(1L);
        history.setFromStatus("DRAFT");
        history.setToStatus("ANALYZING");
        history.setReason("开始分析");
        history.setOperator("张三");
        history.setCreatedAt(LocalDateTime.now());

        Page<WorkItemStatusHistoryEntity> page = new Page<>(1, 20);
        page.setRecords(Arrays.asList(history));
        page.setTotal(1L);

        when(historyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResp<WorkItemStatusHistoryRespVO> result = transitionService.listHistory(1L, 1, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("DRAFT", result.getRecords().get(0).getFromStatus());
    }

    @Test
    @DisplayName("空流转历史")
    void shouldReturnEmptyHistory() {
        Page<WorkItemStatusHistoryEntity> page = new Page<>(1, 20);
        page.setRecords(null);
        page.setTotal(0L);

        when(historyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResp<WorkItemStatusHistoryRespVO> result = transitionService.listHistory(1L, 1, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }
}