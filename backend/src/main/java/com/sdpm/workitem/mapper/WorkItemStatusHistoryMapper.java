package com.sdpm.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdpm.workitem.entity.WorkItemStatusHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkItemStatusHistoryMapper extends BaseMapper<WorkItemStatusHistoryEntity> {
}