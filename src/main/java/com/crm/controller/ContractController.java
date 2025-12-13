package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.enums.BusinessType;
import com.crm.query.ApprovalQuery;
import com.crm.query.ContractQuery;
import com.crm.query.ContractTrendQuery;
import com.crm.query.IdQuery;
import com.crm.service.ContractService;
import com.crm.vo.ContractVO;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@Tag(name="合同管理")
@RestController
@RequestMapping("contract")
@AllArgsConstructor
public class ContractController {
    private final ContractService contractService;

    @PostMapping("page")
    @Operation(summary = "合同列表-分页")
    @Log(title = "合同管理-分页查询", businessType = BusinessType.SELECT)
    public Result<PageResult<ContractVO>> getPage(@RequestBody @Validated ContractQuery contractQuery){
        return Result.ok(contractService.getPage(contractQuery));
    }

    @PostMapping("saveOrUpdate")
    @Operation(summary = "新增/修改合同信息")
    @Log(title = "合同管理-新增/修改合同", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result saveOrUpdate(@RequestBody @Validated ContractVO contractVO){
        contractService.saveOrUpdate(contractVO);
        return Result.ok();
    }

    @PostMapping("getContractTrendData")
    @Operation(summary = "合同数量统计")
    @Log(title = "合同管理-合同数量统计", businessType = BusinessType.OTHER)
    public Result<Map<String,Object >> getContractTrendData(@RequestBody ContractTrendQuery query) {
        Map<String, Object> result = new HashMap<>();
        // 趋势数据（时间轴）
        result.put("trendData", contractService.getContractTrendData(query));
        // 饼图数据
        result.put("pieData", contractService.getContractPieData(query));
        return Result.ok(result);
        //return Result.ok(contractService.getContractTrendData(query));
    }

    @PostMapping("startApproval")
    @Operation(summary = "发起合同审核")
    @Log(title = "合同管理-发起合同审核", businessType = BusinessType.OTHER)
    public Result startApproval(@RequestBody @Validated IdQuery idQuery) {
        contractService.startApproval(idQuery);
        return Result.ok();
    }

    @PostMapping("approvalContract")
    @Operation(summary = "审核合同")
    @Log(title = "合同管理-审核合同", businessType = BusinessType.OTHER)
    public Result approvalContract(@RequestBody @Validated ApprovalQuery query) {
        contractService.approvalContract(query);
        return Result.ok();
    }
}
