package com.sdpm.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.dto.DictCreateReqDTO;
import com.sdpm.workitem.dto.DictUpdateReqDTO;
import com.sdpm.workitem.entity.DictItemEntity;
import com.sdpm.workitem.entity.WorkItemEntity;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.DictItemMapper;
import com.sdpm.workitem.mapper.WorkItemMapper;
import com.sdpm.workitem.service.DictService;
import com.sdpm.workitem.vo.DictRespVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictServiceImpl implements DictService {

    private final DictItemMapper dictItemMapper;
    private final WorkItemMapper workItemMapper;

    public DictServiceImpl(DictItemMapper dictItemMapper, WorkItemMapper workItemMapper) {
        this.dictItemMapper = dictItemMapper;
        this.workItemMapper = workItemMapper;
    }

    @Override
    public List<DictRespVO> listByType(String type, Boolean enabledOnly) {
        LambdaQueryWrapper<DictItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItemEntity::getType, type);
        if (enabledOnly == null || enabledOnly) {
            wrapper.eq(DictItemEntity::getEnabled, 1);
        }
        wrapper.orderByAsc(DictItemEntity::getSort);
        List<DictItemEntity> entities = dictItemMapper.selectList(wrapper);
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DictRespVO createDict(DictCreateReqDTO dto) {
        DictItemEntity entity = new DictItemEntity();
        entity.setType(dto.getType());
        entity.setKey(dto.getKey());
        entity.setLabel(dto.getLabel());
        entity.setValue(dto.getValue());
        entity.setSort(dto.getSort() != null ? dto.getSort() : Integer.valueOf(0));
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0);

        try {
            dictItemMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // uk_dict_item_type_key 兜底
            throw new BizException(ErrorCode.BIZ_DUPLICATE_DICT_KEY);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public DictRespVO updateDict(Long id, DictUpdateReqDTO dto) {
        DictItemEntity entity = dictItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }

        if (dto.getLabel() != null) {
            entity.setLabel(dto.getLabel());
        }
        if (dto.getValue() != null) {
            entity.setValue(dto.getValue());
        }
        if (dto.getSort() != null) {
            entity.setSort(dto.getSort());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0);
        }
        int rows = dictItemMapper.updateById(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.BIZ_VERSION_CONFLICT);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public boolean deleteDict(Long id) {
        DictItemEntity entity = dictItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }
        // work_item 中存的是 dict_item 的原始 key 字符串（status/type/priority/...），
        // dict_item.type 指明该 key 落到了 work_item 的哪一列；
        // 真实生产可在引用表加上外键列后改为按外键 ID 查，此处先按 type 路由到对应字段。
        LambdaQueryWrapper<WorkItemEntity> ref = new LambdaQueryWrapper<>();
        String dictType = entity.getType();
        boolean routed = false;
        if ("status".equalsIgnoreCase(dictType)) {
            ref.eq(WorkItemEntity::getStatus, entity.getKey());
            routed = true;
        } else if ("type".equalsIgnoreCase(dictType)) {
            ref.eq(WorkItemEntity::getType, entity.getKey());
            routed = true;
        } else if ("priority".equalsIgnoreCase(dictType)) {
            ref.eq(WorkItemEntity::getPriority, entity.getKey());
            routed = true;
        }
        if (routed) {
            ref.last("LIMIT 1");
            if (workItemMapper.selectCount(ref) > 0) {
                throw new BizException(ErrorCode.BIZ_DICT_IN_USE,
                    "字典项 " + entity.getType() + ":" + entity.getKey() + " 被工作项引用，无法删除");
            }
        }
        int rows = dictItemMapper.deleteById(id);
        return rows > 0;
    }

    private DictRespVO toVO(DictItemEntity entity) {
        DictRespVO vo = new DictRespVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setKey(entity.getKey());
        vo.setLabel(entity.getLabel());
        vo.setValue(entity.getValue());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        return vo;
    }
}