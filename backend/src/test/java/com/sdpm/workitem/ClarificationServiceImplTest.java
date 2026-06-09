package com.sdpm.workitem;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.ClarificationCreateReqDTO;
import com.sdpm.workitem.dto.ClarificationResolveReqDTO;
import com.sdpm.workitem.entity.ClarificationQuestionEntity;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.ClarificationQuestionMapper;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.service.impl.ClarificationServiceImpl;
import com.sdpm.workitem.vo.ClarificationRespVO;

@ExtendWith(MockitoExtension.class)
class ClarificationServiceImplTest {

    @Mock
    private ClarificationQuestionMapper clarificationQuestionMapper;

    @Mock
    private WorkItemMapper workItemMapper;

    @InjectMocks
    private ClarificationServiceImpl clarificationService;

    private WorkItemEntity workItem;
    private ClarificationQuestionEntity questionEntity;

    @BeforeEach
    void setUp() {
        workItem = new WorkItemEntity();
        workItem.setId(1L);
        workItem.setTitle("测试工作项");
        workItem.setDeleted(0);

        questionEntity = new ClarificationQuestionEntity();
        questionEntity.setId(1L);
        questionEntity.setWorkItemId(1L);
        questionEntity.setQuestion("测试澄清问题");
        questionEntity.setSeverity("P0");
        questionEntity.setStatus("OPEN");
        questionEntity.setRaisedBy("王五");
        questionEntity.setCreatedAt(LocalDateTime.now());
    }

    // ========== 添加澄清问题 ==========
    @Test
    @DisplayName("添加澄清问题成功")
    void shouldAddQuestion() {
        ClarificationCreateReqDTO dto = new ClarificationCreateReqDTO();
        dto.setQuestion("测试澄清问题");
        dto.setSeverity("P0");
        dto.setRaisedBy("王五");

        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(clarificationQuestionMapper.insert(any(ClarificationQuestionEntity.class))).thenReturn(1);

        ClarificationRespVO result = clarificationService.addQuestion(1L, dto);

        assertNotNull(result);
        assertEquals("测试澄清问题", result.getQuestion());
        assertEquals("P0", result.getSeverity());
        assertEquals("OPEN", result.getStatus());
    }

    @Test
    @DisplayName("添加重复问题 → 抛BIZ_DUPLICATE_QUESTION")
    void shouldRejectDuplicateQuestion() {
        ClarificationCreateReqDTO dto = new ClarificationCreateReqDTO();
        dto.setQuestion("重复问题");

        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_clarification_question_work_item_question"))
                .when(clarificationQuestionMapper).insert(any(ClarificationQuestionEntity.class));

        BizException ex = assertThrows(BizException.class,
                () -> clarificationService.addQuestion(1L, dto));
        assertEquals(ErrorCode.BIZ_DUPLICATE_QUESTION.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("为不存在的工作项添加问题 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnAddToMissingWorkItem() {
        ClarificationCreateReqDTO dto = new ClarificationCreateReqDTO();
        dto.setQuestion("测试问题");
        when(workItemMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> clarificationService.addQuestion(999L, dto));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("默认严重程度为P1")
    void shouldDefaultToP1Severity() {
        ClarificationCreateReqDTO dto = new ClarificationCreateReqDTO();
        dto.setQuestion("无严重程度的澄清问题");
        dto.setSeverity(null);

        when(workItemMapper.selectById(1L)).thenReturn(workItem);
        when(clarificationQuestionMapper.insert(any(ClarificationQuestionEntity.class))).thenReturn(1);

        ClarificationRespVO result = clarificationService.addQuestion(1L, dto);

        assertEquals("P1", result.getSeverity());
    }

    // ========== 解决澄清问题 ==========
    @Test
    @DisplayName("解决澄清问题成功")
    void shouldResolveQuestion() {
        ClarificationResolveReqDTO dto = new ClarificationResolveReqDTO();
        dto.setAnswer("已解决，这是答案");
        dto.setResolvedBy("管理员");

        when(clarificationQuestionMapper.selectById(1L)).thenReturn(questionEntity);
        when(clarificationQuestionMapper.updateById(any(ClarificationQuestionEntity.class))).thenReturn(1);

        ClarificationRespVO result = clarificationService.resolveQuestion(1L, dto);

        assertNotNull(result);
        assertEquals("已解决，这是答案", result.getAnswer());
        assertEquals("RESOLVED", result.getStatus());
    }

    @Test
    @DisplayName("解决不存在的澄清问题 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnResolveMissing() {
        ClarificationResolveReqDTO dto = new ClarificationResolveReqDTO();
        dto.setAnswer("答案");
        when(clarificationQuestionMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> clarificationService.resolveQuestion(999L, dto));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("重复解决 → 抛BIZ_CLARIFICATION_ALREADY_RESOLVED")
    void shouldRejectDoubleResolve() {
        questionEntity.setStatus("RESOLVED");
        ClarificationResolveReqDTO dto = new ClarificationResolveReqDTO();
        dto.setAnswer("重复答案");
        when(clarificationQuestionMapper.selectById(1L)).thenReturn(questionEntity);

        BizException ex = assertThrows(BizException.class,
                () -> clarificationService.resolveQuestion(1L, dto));
        assertEquals(ErrorCode.BIZ_CLARIFICATION_ALREADY_RESOLVED.getCode(), ex.getErrorCode().getCode());
    }

    // ========== 列表查询 ==========
    @Test
    @DisplayName("查询澄清问题列表")
    void shouldListQuestions() {
        Page<ClarificationQuestionEntity> page = new Page<>(1, 20);
        page.setRecords(Arrays.asList(questionEntity));
        page.setTotal(1L);

        when(clarificationQuestionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResp<ClarificationRespVO> result = clarificationService.listQuestions(1L, null, null, 1, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    // ========== 统计P0未解决 ==========
    @Test
    @DisplayName("统计P0未解决问题数量")
    void shouldCountP0Open() {
        when(clarificationQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        long count = clarificationService.countP0Open(1L);

        assertEquals(3L, count);
    }

    @Test
    @DisplayName("P0已解决则计数为0")
    void shouldCountZeroWhenAllResolved() {
        when(clarificationQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        long count = clarificationService.countP0Open(1L);

        assertEquals(0L, count);
    }
}