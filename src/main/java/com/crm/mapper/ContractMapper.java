package com.crm.mapper;

import com.crm.entity.Contract;
import com.crm.query.ContractTrendQuery;
import com.crm.vo.ContractTrendVO;
import com.crm.vo.PieDataVO;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */
public interface ContractMapper extends MPJBaseMapper<Contract> {
    /**
     * 合同交易趋势统计（通用/汇总维度）
     */
    List<ContractTrendVO> getTradeStatistics(@Param("query") ContractTrendQuery query);

    /**
     * 合同交易趋势按日统计
     */
    List<ContractTrendVO> getTradeStatisticsByDay(@Param("query") ContractTrendQuery query);

    /**
     * 合同交易趋势按周统计
     */
    List<ContractTrendVO> getTradeStatisticsByWeek(@Param("query") ContractTrendQuery query);
    // ========== 饼图统计 ==========
    /**
     * 按合同状态统计（0-初始化 1-审核通过 2-审核未通过）
     */
    List<PieDataVO> getContractPieByStatus(@Param("query") ContractTrendQuery query);

    /**
     * 按合同金额区间统计
     */
    List<PieDataVO> getContractPieByAmount(@Param("query") ContractTrendQuery query);

    /**
     * 按归属客户统计（关联客户表）
     */
    List<PieDataVO> getContractPieByCustomer(@Param("query") ContractTrendQuery query);
}
