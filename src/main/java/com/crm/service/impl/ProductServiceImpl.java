package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.entity.Product;
import com.crm.mapper.ProductMapper;
import com.crm.query.ProductQuery;
import com.crm.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    @Override
    public PageResult<Product> getPage(ProductQuery query) {
        // 1. 构造分页参数
        Page<Product> page = new Page<>(query.getPage(), query.getLimit());

        // 2. 构造查询条件
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getDeleteFlag, 0); // 未删除
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Product::getName, query.getName()); // 商品名称模糊搜索
        }
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus()); // 商品状态精确匹配
        }
        wrapper.orderByDesc(Product::getCreateTime); // 按创建时间倒序

        // 3. 执行分页查询
        Page<Product> result = baseMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(),page.getTotal());
    }

    @Override
    public void saveOrEdit(Product product) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getName, product.getName());
        if (product.getId() == null) {
            Product newProduct = baseMapper.selectOne(wrapper);
            if (newProduct != null){
                throw new ServerException("该商品名称已存在，请勿重复添加");
            }
            baseMapper.insert(product);
        }else {
            wrapper.ne(Product::getId, product.getId());
            Product oldProduct = baseMapper.selectOne(wrapper);
            if (oldProduct != null){
                throw new ServerException("该商品名称已存在，请勿重复添加");
            }
            baseMapper.updateById(product);
        }
    }

    @Override
    public void batchUpdateProductState() {

        //定时下架时间早于当前定时任务执行的时间，修改商品状态
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .lt(Product::getOffShelfTime, LocalDateTime.now());
        Product offProduct = new Product();
        offProduct.setStatus(2);
        offProduct.setOffShelfTime(null);
        baseMapper.update(offProduct, wrapper);

        wrapper.clear();
        wrapper.lt(Product::getOnShelfTime, LocalDateTime.now());
        Product onProduct = new Product();
        onProduct.setStatus(1);
        onProduct.setOnShelfTime(null);
        baseMapper.update(onProduct, wrapper);
    }
}
