package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.ContractConvert;
import com.crm.entity.*;
import com.crm.mapper.*;
import com.crm.query.*;
import com.crm.security.user.SecurityUser;
import com.crm.service.ContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.utils.DateUtils;
import com.crm.utils.EmailUtils;
import com.crm.vo.*;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static com.crm.utils.DateUtils.*;
import static com.crm.utils.NumberUtils.generateContractNumber;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
@AllArgsConstructor
@Slf4j
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {
    private final ContractProductMapper contractProductMapper;
    private final ProductMapper ProductMapper;
    private final ApprovalMapper approvalMapper;
    private final ManagerMapper managerMapper;
    private final EmailUtils emailUtils;
    @Override
    public PageResult<ContractVO> getPage(ContractQuery query) {
        Page<ContractVO> page = new Page(query.getPage(),query.getLimit());
        MPJLambdaWrapper<Contract> wrapper = new MPJLambdaWrapper<>();
        if(StringUtils.isNotBlank(query.getName())){
            wrapper.like(Contract::getName, query.getName());
        }
        if (query.getStatus() != null){
            wrapper.eq(Contract::getStatus, query.getStatus());
        }
        if (query.getCustomerId() != null){
            wrapper.eq(Contract::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.isNotBlank(query.getNumber())){
            wrapper.like(Contract::getNumber, query.getNumber());
        }

        //wrapper.orderByDesc(Contract::getCreateTime);
        Integer managerId = SecurityUser.getManagerId();
        wrapper.selectAll(Contract.class)
                .selectAs(Customer::getName,ContractVO::getCustomerName)
                .leftJoin(Customer.class, Customer::getId, Contract::getCustomerId)
                .eq(Contract::getOwnerId, managerId).orderByDesc(Contract::getCreateTime);

        Page<ContractVO> result = baseMapper.selectJoinPage(page, ContractVO.class, wrapper);
        if (!result.getRecords().isEmpty()){
            result.getRecords().forEach(contractVO -> {
                List<ContractProduct> contractProducts = contractProductMapper.selectList(new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractVO.getId()));
                contractVO.setProducts(ContractConvert.INSTANCE.convertToProductVOList(contractProducts));
            });
        }
        return new PageResult<>(result.getRecords(),page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(ContractVO contractVO){
        boolean isNew = contractVO.getId() == null;
        if (isNew && baseMapper.exists(new LambdaQueryWrapper<Contract>().eq(Contract::getName, contractVO.getName()))){
            throw new ServerException("合同名称已存在，请勿重复添加");
        }

        //转换并保存合同关系
        Contract contract = ContractConvert.INSTANCE.convert(contractVO);
        contract.setCreaterId(SecurityUser.getManagerId());
        contract.setOwnerId(SecurityUser.getManagerId());
        // 关键：手动设置创建时间和更新时间（覆盖自动填充的问题）
        //LocalDateTime now = LocalDateTime.now();
        if (isNew){
            contract.setNumber(generateContractNumber());
            contract.setReceivedAmount(BigDecimal.ZERO);
            contract.setStatus(0);
            baseMapper.insert(contract);
//            contract.setNumber(generateContractNumber());
//            contract.setCreateTime(now); // 手动赋值创建时间
//            contract.setUpdateTime(now); // 手动赋值更新时间
//            // 显式设置状态为默认值 0

//            baseMapper.insert(contract);
        }else {
            Contract oldContract = baseMapper.selectById(contract.getId());
            if (oldContract == null){
                throw new ServerException("合同不存在");
            }
            if (oldContract.getStatus() == 1){
                throw new ServerException("合同正在审核中，请勿执行修改操作");
            }
            //handleContractProduct(contract.getId(), contractVO.getProducts());
            baseMapper.updateById(contract);
        }
        //处理商品和合同的关联关系
        handleContractProduct(contract.getId(), contractVO.getProducts());
    }
    private void handleContractProduct(Integer contractId, List<ProductVO> newProductList){
        if (newProductList == null)
            return;

        List<ContractProduct> oldProducts = contractProductMapper.selectList(
                new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractId)
        );

        // 1. 新增商品
        List<ProductVO> newAdded = newProductList.stream()
                .filter(np -> oldProducts.stream().noneMatch(op -> op.getPId().equals(np.getId())))
                .toList();
        for (ProductVO p : newAdded) {
            Product product = checkProductStock(p.getId(), p.getCount());
            decreaseStock(product, p.getCount());
            ContractProduct cp = buildContractProduct(contractId, product, p.getCount());
            contractProductMapper.insert(cp);
        }

    //2.修改数量
    List<ProductVO> changed = newProductList.stream()
            .filter(np -> oldProducts.stream()
            .anyMatch(op -> op.getPId().equals(np.getId()) && !op.getCount().equals(np.getCount())))
            .toList();
    for (ProductVO p : changed){
        ContractProduct old = oldProducts.stream()
                .filter(op -> op.getPId().equals(p.getId()))
                .findFirst().orElseThrow();

        Product product = checkProductStock(p.getId(), 0);
        int diff = p.getCount() - old.getCount();

        //库存调整
        if (diff > 0)
            decreaseStock(product, diff);
        else if (diff < 0)
            increaseStock(product, -diff);

        //更新合同商品
        old.setCount(p.getCount());
        old.setPrice(product.getPrice());
        old.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(p.getCount())));
        contractProductMapper.updateById(old);
        }

        //3.删除商品
        List<ContractProduct> removed = oldProducts.stream()
                .filter(op -> newProductList.stream().noneMatch(np -> np.getId().equals(op.getPId())))
                .toList();
        for (ContractProduct rm : removed){
            Product product = ProductMapper.selectById(rm.getPId());
            if (product != null)
                increaseStock(product, rm.getCount());
            contractProductMapper.deleteById(rm.getId());
        }
    }

    private ContractProduct buildContractProduct(Integer contractId, Product product, int count){
        ContractProduct contractProduct = new ContractProduct();
        contractProduct.setCId(contractId); // 构造时就设置合同 ID
        contractProduct.setPId(product.getId());
        contractProduct.setPName(product.getName());
        contractProduct.setPrice(product.getPrice());
        contractProduct.setCount(count);
        contractProduct.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(count)));
        //contractProduct.setTotalPrice(product.getPrice().multiply(new BigDecimal(count)));
        return contractProduct;
    }

    private Product checkProductStock(Integer productId, int count){
        if (productId != null) {
            // 如果传的是合同商品表的主键，先通过合同商品表找到真实的商品ID
            ContractProduct cp = contractProductMapper.selectById(productId);
            if (cp != null) {
                productId = cp.getPId(); // 替换为真实的商品表ID
            }
        }


        Product product = ProductMapper.selectById(productId);
        if (product == null){
            throw new ServerException("商品不存在");
        }
        if(count > 0 && product.getStock() < count){
            throw new ServerException("商品库存不足");
        }
//        if(product.getStock() < count){
//            throw new ServerException("商品库存不足");
//        }
        return product;
    }

    private void increaseStock(Product product, int count){
        product.setStock(product.getStock() + count);
        product.setSales(product.getSales() - count);
        ProductMapper.updateById(product);
    }

    private void decreaseStock(Product product, int count){
        product.setStock(product.getStock() - count);
        product.setSales(product.getSales() + count);
        ProductMapper.updateById(product);
    }

    @Override
    public Map<String, List> getContractTrendData(ContractTrendQuery query) {
        // x轴时间数据
        List<String> timeList = new ArrayList<>();
        // 统计合同数量数据
        List<Integer> countList = new ArrayList<>();
        List<ContractTrendVO> tradeStatistics;

        if ("day".equals(query.getTransactionType())) {
            LocalDateTime now = LocalDateTime.now();
            // 截断毫秒和纳秒部分
            LocalDateTime truncatedNow = now.truncatedTo(ChronoUnit.SECONDS);
            LocalDateTime startTime = now.withHour(0).withMinute(0).withSecond(0).truncatedTo(ChronoUnit.SECONDS);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            List<String> timeRange = new ArrayList<>();
            timeRange.add(formatter.format(startTime));
            timeRange.add(formatter.format(truncatedNow));
            timeList = getHourData(timeRange);
            query.setTimeRange(timeRange);
            tradeStatistics = baseMapper.getTradeStatistics(query);
        } else if ("monthrange".equals(query.getTransactionType())) {
            query.setTimeFormat("'%Y-%m'");
            timeList = getMonthInRange(query.getTimeRange().get(0), query.getTimeRange().get(1));
            tradeStatistics = baseMapper.getTradeStatisticsByDay(query);
        } else if ("week".equals(query.getTransactionType())) {
            timeList = getWeekInRange(query.getTimeRange().get(0), query.getTimeRange().get(1));
            tradeStatistics = baseMapper.getTradeStatisticsByWeek(query);
        } else {
            query.setTimeFormat("'%Y-%m-%d'");
            timeList = DateUtils.getDatesInRange(query.getTimeRange().get(0), query.getTimeRange().get(1));
            tradeStatistics = baseMapper.getTradeStatisticsByDay(query);
        }

        // 匹配时间点数据，无数据则补0
        List<ContractTrendVO> finalTradeStatistics = tradeStatistics;
        timeList.forEach(item -> {
            ContractTrendVO statisticsVO = finalTradeStatistics.stream()
                    .filter(vo -> {
                        if ("day".equals(query.getTransactionType())) {
                            // 比较小时段
                            return item.substring(0, 2).equals(vo.getTradeTime().substring(0, 2));
                        } else {
                            return item.equals(vo.getTradeTime());
                        }
                    })
                    .findFirst()
                    .orElse(null);
            if (statisticsVO != null) {
                countList.add(statisticsVO.getTradeCount());
            } else {
                countList.add(0);
            }
        });

        Map<String, List> result = new HashMap<>();
        result.put("timeList", timeList);
        result.put("countList", countList);
        return result;
    }

    @Override
    public List<PieDataVO> getContractPieData(ContractTrendQuery query) {
        switch (query.getDimension()) {
            case "status":
                return baseMapper.getContractPieByStatus(query);
            case "amount":
                return baseMapper.getContractPieByAmount(query);
            case "customer":
                return baseMapper.getContractPieByCustomer(query);
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public void startApproval(IdQuery idQuery) {
        Contract contract = baseMapper.selectById(idQuery.getId());
        if (contract == null) {
            throw new ServerException("合同不存在");
        }
        if (contract.getStatus() != 0) {
            throw new ServerException("该合同已审核通过，请勿重复提交");
        }
        contract.setStatus(1);
        baseMapper.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvalContract(ApprovalQuery query) {
// 根据id 查询合同信息
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null) {
            throw new ServerException("合同不存在");
        }
        if (contract.getStatus() != 1) {
            throw new ServerException("该合同已审核通过，请勿重复提交");
        }
// 审核内容
        String approvalContent = query.getComment(); // 直接取自定义意见
        //String approvalContent = query.getType() == 0 ? "审核通过" : "审核拒绝";
// 合同列表审核状态
        Integer contractStatus = query.getType() == 0 ? 2 : 3;
        contract.setStatus(contractStatus);
// 审核记录
        Approval approval = new Approval();
        approval.setType(0);
        approval.setStatus(query.getType());
        approval.setCreaterId(SecurityUser.getManagerId());
        approval.setContractId(query.getId());
        approval.setDeleteFlag(0);
        approval.setComment(approvalContent);
        baseMapper.updateById(contract);
        approvalMapper.insert(approval);

        try {
            // 获取合同创建人信息
            Manager creator = managerMapper.selectById(contract.getCreaterId());
            if (creator == null || StringUtils.isBlank(creator.getEmail())) {
                log.warn("合同创建人邮箱不存在");
                return;
            }

            // 发送邮件
            String toEmail = creator.getEmail();
            String contractName = contract.getName();
            String contractNumber = contract.getNumber();

            if (query.getType() == 0) {
                emailUtils.sendContractApprovedEmail(toEmail, contractName, approvalContent, contractNumber);
            } else {
                emailUtils.sendContractRejectedEmail(toEmail, contractName, approvalContent, contractNumber);
            }
        } catch (Exception e) {
            log.error("发送审核邮件失败");

        }
    }
}
