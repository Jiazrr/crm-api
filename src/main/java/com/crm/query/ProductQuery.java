package com.crm.query;

import com.crm.common.model.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProductQuery extends Query {
    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "商品状态（0-初始化，1-上架，2-下架）")
    private Integer status;
}
