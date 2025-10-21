package com.crm.convert;

import com.crm.entity.SysManager;
import com.crm.security.user.ManagerDetail;
import com.crm.vo.SysManagerVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;


@Mapper
public interface SysManagerConvert {
    SysManagerConvert INSTANCE = Mappers.getMapper(SysManagerConvert.class);

    SysManager convert(SysManagerVO vo);

    // 关键：显式映射departId字段（源：SysManager的departId → 目标：ManagerDetail的departId）
    @Mapping(source = "departId", target = "departId")
    ManagerDetail convertDetail(SysManager entity);

    List<SysManagerVO> convertList(List<SysManager> list);
}
