package com.sdpm.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.dto.DictCreateReqDTO;
import com.sdpm.workitem.dto.DictUpdateReqDTO;
import com.sdpm.workitem.entity.DictItemEntity;
import com.sdpm.workitem.exception.BizException;
import com.sdpm.workitem.mapper.DictItemMapper;
import com.sdpm.workitem.service.DictService;
import com.sdpm.workitem.vo.DictRespVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictServiceImpl implements DictService {

    private final DictItemMapper dictItemMapper;

    public DictServiceImpl(DictItemMapper dictItemMapper) {
        this.dictItemMapper = dictItemMapper;
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
    public DictRespVO createDict(DictCreateReqDTO dto) {
        LambdaQueryWrapper<DictItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItemEntity::getType, dto.getType())
                .eq(DictItemEntity::getKey, dto.getKey());
        if (dictItemMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BIZ_DUPLICATE_DICT_KEY);
        }

        DictItemEntity entity = new DictItemEntity();
        entity.setType(dto.getType());
        entity.setKey(dto.getKey());
        entity.setLabel(dto.getLabel());
        entity.setValue(dto.getValue());
        entity.setSort(dto.getSort() != null ? dto.getSort() : Integer.valueOf(0));
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0);
        dictItemMapper.insert(entity);
        return toVO(entity);
    }

    @Override
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
        dictItemMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public boolean deleteDict(Long id) {
        DictItemEntity entity = dictItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_NOT_FOUND);
        }
        dictItemMapper.deleteById(id);
        return true;
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