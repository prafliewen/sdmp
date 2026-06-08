package com.sdpm.workitem;

import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.ClarificationQuestionMapper;
import com.sdpm.workitem.service.P0ClarificationGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class P0ClarificationGuardTest {

    @Mock
    private ClarificationQuestionMapper clarificationQuestionMapper;

    @InjectMocks
    private P0ClarificationGuard guard;

    private WorkItemEntity workItem;

    @BeforeEach
    void setUp() {
        workItem = new WorkItemEntity();
        workItem.setId(1L);
        workItem.setStatus("ANALYZING");
    }

    @Test
    @DisplayName("P0 拦截: 存在未解决P0 → 进入READY被阻断")
    void shouldBlockWhenP0OpenExists() {
        when(clarificationQuestionMapper.selectCount(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
            () -> guard.check(workItem, WorkItemStatusEnum.READY));
        assertEquals(ErrorCode.BIZ_P0_CLARIFICATION_BLOCKED.getCode(), ex.getErrorCode().getCode());
        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    @DisplayName("P0 拦截: 存在P0 → 进入IN_PROGRESS被阻断")
    void shouldBlockInProgress() {
        when(clarificationQuestionMapper.selectCount(any())).thenReturn(2L);

        BizException ex = assertThrows(BizException.class,
            () -> guard.check(workItem, WorkItemStatusEnum.IN_PROGRESS));
        assertEquals(ErrorCode.BIZ_P0_CLARIFICATION_BLOCKED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("P0 拦截: 存在P0 → 进入IN_TESTING被阻断")
    void shouldBlockInTesting() {
        when(clarificationQuestionMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BizException.class,
            () -> guard.check(workItem, WorkItemStatusEnum.IN_TESTING));
    }

    @Test
    @DisplayName("P0 拦截: 存在P0 → 进入DONE被阻断")
    void shouldBlockDone() {
        when(clarificationQuestionMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BizException.class,
            () -> guard.check(workItem, WorkItemStatusEnum.DONE));
    }

    @Test
    @DisplayName("放行: 无P0 OPEN → 进入READY正常")
    void shouldPassWhenNoP0Open() {
        when(clarificationQuestionMapper.selectCount(any())).thenReturn(0L);

        assertDoesNotThrow(() -> guard.check(workItem, WorkItemStatusEnum.READY));
    }

    @Test
    @DisplayName("放行: 非拦截目标状态(DRAFT)不检查P0")
    void shouldPassForNonBlockedStatus() {
        assertDoesNotThrow(() -> guard.check(workItem, WorkItemStatusEnum.DRAFT));
    }
}