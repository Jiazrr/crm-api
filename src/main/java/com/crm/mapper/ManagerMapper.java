package com.crm.mapper;

import com.crm.entity.Manager;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 用户管理 Mapper 接口
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface ManagerMapper extends BaseMapper<Manager> {
    // 查询创建人的邮箱
    @Select("SELECT email FROM sys_manager WHERE id = #{createrId}")
    String getCreaterEmail(Long createrId);
}
