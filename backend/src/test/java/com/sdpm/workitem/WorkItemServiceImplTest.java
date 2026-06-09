package com.sdpm.workitem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.PageResp;
import com.sdpm.workitem.dto.WorkItemCreateReqDTO;
import com.sdpm.workitem.dto.WorkItemQueryReqDTO;
import com.sdpm.workitem.dto.WorkItemUpdateReqDTO;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.entity.WorkItemStatusHistoryEntity;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.mapper.WorkItemStatusHistoryMapper;
import com.sdpm.workitem.service.ClarificationService;
import com.sdpm.workitem.service.impl.WorkItemServiceImpl;
import com.sdpm.workitem.vo.WorkItemDetailRespVO;
import com.sdpm.workitem.vo.WorkItemRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceImplTest {

    @Mock
    private WorkItemMapper workItemMapper;

    @Mock
    private WorkItemStatusHistoryMapper workItemStatusHistoryMapper;

    @Mock
    private ClarificationService clarificationService;

    @InjectMocks
    private WorkItemServiceImpl workItemService;

    private WorkItemCreateReqDTO createReq;
    private WorkItemEntity savedEntity;

    @BeforeEach
    void setUp() {
        createReq = new WorkItemCreateReqDTO();
        createReq.setTitle("测试工作项");
        createReq.setType("STORY");
        createReq.setPriority("P1");
        createReq.setDescription("测试描述");
        createReq.setAssignee("张三");
        createReq.setReporter("李四");
        createReq.setTags(Arrays.asList("tag1", "tag2"));
        createReq.setAcceptanceCriteria(Arrays.asList("验收条件1", "验收条件2"));

        savedEntity = new WorkItemEntity();
        savedEntity.setId(1L);
        savedEntity.setCode("WI-20260608-0001");
        savedEntity.setTitle("测试工作项");
        savedEntity.setType("STORY");
        savedEntity.setPriority("P1");
        savedEntity.setStatus("DRAFT");
        savedEntity.setVersion(0L);
        savedEntity.setCreatedAt(LocalDateTime.now());
        savedEntity.setUpdatedAt(LocalDateTime.now());
    }

    private void mockInsertSuccess() {
        doAnswer(inv -> {
            WorkItemEntity e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        }).when(workItemMapper).insert(ArgumentMatchers.<WorkItemEntity>any());
        doReturn(1).when(workItemStatusHistoryMapper).insert(ArgumentMatchers.<WorkItemStatusHistoryEntity>any());
    }

    // ========== 创建 ==========
    @Test
    @DisplayName("创建工作项成功 → 返回完整VO")
    void shouldCreateWorkItemSuccessfully() {
        mockInsertSuccess();

        WorkItemRespVO result = workItemService.createWorkItem(createReq);

        assertNotNull(result);
        assertEquals("测试工作项", result.getTitle());
        assertEquals("STORY", result.getType());
        assertEquals("DRAFT", result.getStatus());

        // 防止回归：insert 之前 entity 必须把所有字段填齐
        ArgumentCaptor<WorkItemEntity> captor = ArgumentCaptor.forClass(WorkItemEntity.class);
        verify(workItemMapper).insert(captor.capture());
        WorkItemEntity inserted = captor.getValue();
        assertEquals("STORY", inserted.getType());
        assertEquals("P1", inserted.getPriority());
        assertEquals("DRAFT", inserted.getStatus());
        assertNotNull(inserted.getTags());
        assertNotNull(inserted.getAcceptanceCriteria());
    }

    @Test
    @DisplayName("创建时自动生成编号")
    void shouldAutoGenerateCode() {
        createReq.setCode(null);
        mockInsertSuccess();
        doReturn(null).when(workItemMapper).selectMaxCodeByPrefix(ArgumentMatchers.anyString());

        WorkItemRespVO result = workItemService.createWorkItem(createReq);

        assertNotNull(result.getCode());
        assertTrue(result.getCode().startsWith("WI-"));
    }

    @Test
    @DisplayName("基于当日最大编号自增")
    void shouldIncrementCodeFromLatest() {
        createReq.setCode(null);
        mockInsertSuccess();
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        doReturn("WI-" + today + "-000042").when(workItemMapper).selectMaxCodeByPrefix(ArgumentMatchers.anyString());

        WorkItemRespVO result = workItemService.createWorkItem(createReq);

        assertEquals("WI-" + today + "-000043", result.getCode());
    }

    @Test
    @DisplayName("创建时使用指定编号")
    void shouldUseSpecifiedCode() {
        createReq.setCode("CUSTOM-001");
        mockInsertSuccess();

        WorkItemRespVO result = workItemService.createWorkItem(createReq);

        assertEquals("CUSTOM-001", result.getCode());
    }

    @Test
    @DisplayName("code生成遇唯一键冲突时自动重试")
    void shouldRetryOnDuplicateCode() {
        createReq.setCode(null);
        // 第一次 insert 抛 DuplicateKeyException（模拟并发碰撞），第二次成功
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_work_item_code"))
                .doAnswer(inv -> {
                    WorkItemEntity e = inv.getArgument(0);
                    e.setId(1L);
                    return 1;
                })
                .when(workItemMapper).insert(ArgumentMatchers.<WorkItemEntity>any());
        doReturn(1).when(workItemStatusHistoryMapper).insert(ArgumentMatchers.<WorkItemStatusHistoryEntity>any());
        doReturn(null).when(workItemMapper).selectMaxCodeByPrefix(ArgumentMatchers.anyString());

        WorkItemRespVO result = workItemService.createWorkItem(createReq);

        assertNotNull(result.getCode());
        verify(workItemMapper, times(2)).insert(ArgumentMatchers.<WorkItemEntity>any());
    }

    // ========== 更新 ==========
    @Test
    @DisplayName("更新工作项成功")
    void shouldUpdateWorkItemSuccessfully() {
        WorkItemUpdateReqDTO updateReq = new WorkItemUpdateReqDTO();
        updateReq.setTitle("更新后的标题");
        updateReq.setVersion(0L);

        doReturn(savedEntity).when(workItemMapper).selectById(1L);
        doReturn(1).when(workItemMapper).updateById(ArgumentMatchers.<WorkItemEntity>any());

        WorkItemRespVO result = workItemService.updateWorkItem(1L, updateReq);

        assertEquals("更新后的标题", result.getTitle());

        // captor 验证 updateById 传入的 entity 包含正确的 title 和 version
        ArgumentCaptor<WorkItemEntity> captor = ArgumentCaptor.forClass(WorkItemEntity.class);
        verify(workItemMapper).updateById(captor.capture());
        WorkItemEntity updated = captor.getValue();
        assertEquals("更新后的标题", updated.getTitle());
        assertEquals(0L, updated.getVersion());
    }

    @Test
    @DisplayName("更新不存在的项 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnUpdate() {
        WorkItemUpdateReqDTO updateReq = new WorkItemUpdateReqDTO();
        updateReq.setVersion(0L);
        doReturn(null).when(workItemMapper).selectById(999L);

        BizException ex = assertThrows(BizException.class,
                () -> workItemService.updateWorkItem(999L, updateReq));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("更新DONE状态 → 抛BIZ_DONE_IMMUTABLE")
    void shouldRejectUpdateDone() {
        savedEntity.setStatus("DONE");
        WorkItemUpdateReqDTO updateReq = new WorkItemUpdateReqDTO();
        updateReq.setVersion(0L);
        doReturn(savedEntity).when(workItemMapper).selectById(1L);

        BizException ex = assertThrows(BizException.class,
                () -> workItemService.updateWorkItem(1L, updateReq));
        assertEquals(ErrorCode.BIZ_DONE_IMMUTABLE.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("乐观锁冲突 → 抛BIZ_VERSION_CONFLICT")
    void shouldThrowVersionConflict() {
        WorkItemUpdateReqDTO updateReq = new WorkItemUpdateReqDTO();
        updateReq.setVersion(0L);
        doReturn(savedEntity).when(workItemMapper).selectById(1L);
        doReturn(0).when(workItemMapper).updateById(ArgumentMatchers.<WorkItemEntity>any());

        BizException ex = assertThrows(BizException.class,
                () -> workItemService.updateWorkItem(1L, updateReq));
        assertEquals(ErrorCode.BIZ_VERSION_CONFLICT.getCode(), ex.getErrorCode().getCode());
    }

    // ========== 查询详情 ==========
    @Test
    @DisplayName("查询详情成功")
    void shouldGetDetail() {
        doReturn(savedEntity).when(workItemMapper).selectById(1L);

        WorkItemDetailRespVO result = workItemService.getWorkItemDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试工作项", result.getTitle());
    }

    @Test
    @DisplayName("查询不存在的详情 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnDetail() {
        doReturn(null).when(workItemMapper).selectById(999L);

        BizException ex = assertThrows(BizException.class,
                () -> workItemService.getWorkItemDetail(999L));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    // ========== 分页查询 ==========
    @Test
    @DisplayName("分页查询返回正确分页结构")
    void shouldPageWorkItems() {
        WorkItemQueryReqDTO queryReq = new WorkItemQueryReqDTO();
        queryReq.setPageNo(1);
        queryReq.setPageSize(10);

        Page<WorkItemEntity> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(savedEntity));
        page.setTotal(1L);

        doReturn(page).when(workItemMapper).selectPage(ArgumentMatchers.<Page<WorkItemEntity>>any(),
                ArgumentMatchers.<LambdaQueryWrapper<WorkItemEntity>>any());

        PageResp<WorkItemRespVO> result = workItemService.pageWorkItems(queryReq);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("按状态过滤分页")
    void shouldFilterByStatus() {
        WorkItemQueryReqDTO queryReq = new WorkItemQueryReqDTO();
        queryReq.setStatus("DRAFT");

        Page<WorkItemEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);

        doReturn(page).when(workItemMapper).selectPage(ArgumentMatchers.<Page<WorkItemEntity>>any(),
                ArgumentMatchers.<LambdaQueryWrapper<WorkItemEntity>>any());

        PageResp<WorkItemRespVO> result = workItemService.pageWorkItems(queryReq);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    // ========== 软删除 ==========
    @Test
    @DisplayName("软删除成功")
    void shouldSoftDelete() {
        doReturn(savedEntity).when(workItemMapper).selectById(1L);
        doReturn(1).when(workItemMapper).deleteById(ArgumentMatchers.<Long>any());

        assertDoesNotThrow(() -> workItemService.softDeleteWorkItem(1L));

        // 软删除必须走 deleteById（@TableLogic 字段在 updateById 的 SET 子句中会被剔除）
        verify(workItemMapper).deleteById(1L);
    }

    @Test
    @DisplayName("软删除不存在的项 → 抛BIZ_NOT_FOUND")
    void shouldThrowNotFoundOnDelete() {
        doReturn(null).when(workItemMapper).selectById(999L);

        BizException ex = assertThrows(BizException.class,
                () -> workItemService.softDeleteWorkItem(999L));
        assertEquals(ErrorCode.BIZ_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("软删除DONE状态 → 抛BIZ_DONE_IMMUTABLE")
    void shouldRejectDeleteDone() {
        savedEntity.setStatus("DONE");
        doReturn(savedEntity).when(workItemMapper).selectById(1L);

        BizException ex = assertThrows(BizException.class,
                () -> workItemService.softDeleteWorkItem(1L));
        assertEquals(ErrorCode.BIZ_DONE_IMMUTABLE.getCode(), ex.getErrorCode().getCode());
    }
}