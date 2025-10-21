package com.crm.mapper;

import com.crm.entity.Customer;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface CustomerMapper extends MPJBaseMapper<Customer> {
    /**
     * 查询部门及其所有子部门的ID列表（需配合部门表的parent_ids逻辑实现）
     */
    List<Integer> selectSubDeptIds(@Param("deptId") Integer deptId);
}
