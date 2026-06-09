package com.sdpm.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdpm.workitem.entity.WorkItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkItemMapper extends BaseMapper<WorkItemEntity> {

    /**
     * 查找指定前缀下当日最大编号（包含软删除记录）。
     *
     * 使用原生 SQL 是为了绕过 {@code @TableLogic} 过滤：
     * 软删除的记录仍然占用唯一键 uk_work_item_code，
     * 编码生成必须把它们一起算进来，否则会出现"看似没占用、却违反唯一约束"。
     */
    @Select("SELECT code FROM work_item WHERE code LIKE CONCAT(#{prefix}, '%') ORDER BY code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);
}