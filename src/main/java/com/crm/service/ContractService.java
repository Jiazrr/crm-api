package com.crm.service;

import com.crm.common.result.PageResult;
import com.crm.entity.Contract;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crm.query.*;
import com.crm.vo.ContractVO;
import com.crm.vo.PieDataVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface ContractService extends IService<Contract> {
    /**
     *  合同列表-分页
     * @param query
     * @return
     */
    PageResult<ContractVO> getPage(ContractQuery query);

    /**
     *  新增/修改合同
     * @param contractVO
     */
    void saveOrUpdate(ContractVO contractVO);


    /**
     * 合同数量趋势变化
     * @param query
     * @return
     */
    Map<String, List> getContractTrendData(ContractTrendQuery query);

    /**
     * 饼图统计数据
     */
    List<PieDataVO> getContractPieData(ContractTrendQuery query);

    /**
     * 发起合同审核
     *
     * @param idQuery
     */
    void startApproval(IdQuery idQuery);

    /**
     * 审批合同
     * @param query
     */
    void approvalContract(ApprovalQuery query);
}
