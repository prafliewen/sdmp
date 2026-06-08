package com.sdpm.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdpm.workitem.entity.WorkItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkItemMapper extends BaseMapper<WorkItemEntity> {
}