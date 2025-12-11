package com.crm.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractProductVO {
    // 合同商品关联表字段
    @ApiModelProperty("关联表ID")
    private Integer id;
    @ApiModelProperty("合同ID")
    private Integer cId;
    @ApiModelProperty("商品ID")
    private Integer pId;
    @ApiModelProperty("商品名称（关联表）")
    private String pName;
    @ApiModelProperty("单价（关联表）")
    private BigDecimal price;
    @ApiModelProperty("数量（关联表）")
    private Integer count;
    @ApiModelProperty("小计（关联表）")
    private BigDecimal totalPrice;

    // 商品表补充字段
    @ApiModelProperty("销量")
    @TableField(exist = false) // 标记为非数据库字段
    private Integer sales;
    @ApiModelProperty("库存数量")
    @TableField(exist = false)
    private Integer stock;
    @ApiModelProperty("商品状态（0-初始化，1-上架，2-下架）")
    @TableField(exist = false)
    private Integer status;
}
