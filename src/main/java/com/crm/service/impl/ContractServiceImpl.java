package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.ContractConvert;
import com.crm.entity.Contract;
import com.crm.entity.ContractProduct;
import com.crm.entity.Customer;
import com.crm.entity.Product;
import com.crm.mapper.ContractMapper;
import com.crm.mapper.ContractProductMapper;
import com.crm.mapper.ProductMapper;
import com.crm.query.ContractQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.ContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.vo.ContractVO;
import com.crm.vo.ProductVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


import static com.crm.utils.NumberUtils.generateContractNumber;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@AllArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {
    private final ContractProductMapper contractProductMapper;
    private final ProductMapper ProductMapper;
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
}
