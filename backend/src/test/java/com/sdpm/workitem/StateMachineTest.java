package com.sdpm.workitem;

import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.enumeration.WorkItemStatusEnum;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.service.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineTest {

    private StateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new StateMachine();
    }

    // ========== 合法流转 ==========
    @Test
    @DisplayName("合法流转: DRAFT → ANALYZING")
    void shouldAllowDraftToAnalyzing() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.ANALYZING));
        assertDoesNotThrow(() -> stateMachine.assertTransit(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.ANALYZING));
    }

    @Test
    @DisplayName("合法流转: ANALYZING → READY")
    void shouldAllowAnalyzingToReady() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.ANALYZING, WorkItemStatusEnum.READY));
    }

    @Test
    @DisplayName("合法流转: ANALYZING → DRAFT (回退)")
    void shouldAllowAnalyzingToDraft() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.ANALYZING, WorkItemStatusEnum.DRAFT));
    }

    @Test
    @DisplayName("合法流转: READY → IN_PROGRESS")
    void shouldAllowReadyToInProgress() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.READY, WorkItemStatusEnum.IN_PROGRESS));
    }

    @Test
    @DisplayName("合法流转: IN_PROGRESS → IN_TESTING")
    void shouldAllowInProgressToInTesting() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.IN_PROGRESS, WorkItemStatusEnum.IN_TESTING));
    }

    @Test
    @DisplayName("合法流转: IN_TESTING → DONE")
    void shouldAllowInTestingToDone() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.IN_TESTING, WorkItemStatusEnum.DONE));
    }

    @Test
    @DisplayName("合法流转: IN_TESTING → IN_PROGRESS (回退)")
    void shouldAllowInTestingToInProgress() {
        assertTrue(stateMachine.canTransit(WorkItemStatusEnum.IN_TESTING, WorkItemStatusEnum.IN_PROGRESS));
    }

    // ========== 非法流转 ==========
    @Test
    @DisplayName("非法流转: DRAFT → READY (跳级)")
    void shouldRejectDraftToReady() {
        assertFalse(stateMachine.canTransit(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.READY));
        BizException ex = assertThrows(BizException.class,
            () -> stateMachine.assertTransit(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.READY));
        assertEquals(ErrorCode.BIZ_TRANSITION_NOT_ALLOWED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("非法流转: DRAFT → DONE (大跳级)")
    void shouldRejectDraftToDone() {
        BizException ex = assertThrows(BizException.class,
            () -> stateMachine.assertTransit(WorkItemStatusEnum.DRAFT, WorkItemStatusEnum.DONE));
        assertEquals(ErrorCode.BIZ_DONE_IMMUTABLE.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("非法流转: DONE 不可变更")
    void shouldRejectDoneToAny() {
        assertFalse(stateMachine.canTransit(WorkItemStatusEnum.DONE, WorkItemStatusEnum.DRAFT));
        BizException ex = assertThrows(BizException.class,
            () -> stateMachine.assertTransit(WorkItemStatusEnum.DONE, WorkItemStatusEnum.DRAFT));
        assertEquals(ErrorCode.BIZ_DONE_IMMUTABLE.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("非法流转: IN_PROGRESS → DRAFT (跨多级回退)")
    void shouldRejectInProgressToDraft() {
        assertFalse(stateMachine.canTransit(WorkItemStatusEnum.IN_PROGRESS, WorkItemStatusEnum.DRAFT));
    }

    @Test
    @DisplayName("非法流转: READY → DONE (跳级)")
    void shouldRejectReadyToDone() {
        BizException ex = assertThrows(BizException.class,
            () -> stateMachine.assertTransit(WorkItemStatusEnum.READY, WorkItemStatusEnum.DONE));
        assertEquals(ErrorCode.BIZ_DONE_IMMUTABLE.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("DONE 状态无允许的目标状态")
    void doneShouldHaveNoAllowedTransitions() {
        assertTrue(stateMachine.getAllowedTransitions(WorkItemStatusEnum.DONE).isEmpty());
    }
}