package com.crm.mapper;

import com.crm.entity.Customer;
import com.crm.query.CustomerTrendQuery;
import com.crm.vo.CustomerTrendVO;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 */
public interface CustomerMapper extends MPJBaseMapper<Customer> {
    /**
     * 查询部门及其所有子部门的ID列表（需配合部门表的parent_ids逻辑实现）
     */
    List<Integer> selectSubDeptIds(@Param("deptId") Integer deptId);
    /**
     * 客户交易趋势统计（通用/汇总维度）
     */
    List<CustomerTrendVO> getTradeStatistics(@Param("query") CustomerTrendQuery query);

    /**
     * 客户交易趋势按日统计
     */
    List<CustomerTrendVO> getTradeStatisticsByDay(@Param("query") CustomerTrendQuery query);

    /**
     * 客户交易趋势按周统计
     */
    List<CustomerTrendVO> getTradeStatisticsByWeek(@Param("query") CustomerTrendQuery query);


}

