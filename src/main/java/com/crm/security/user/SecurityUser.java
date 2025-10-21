package com.crm.security.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author crm
 */
public class SecurityUser {
    /**
     * 获取用户信息
     */
    public static ManagerDetail getManager() {
        ManagerDetail user;
        try {
            user = (ManagerDetail)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }catch (Exception e){
            return new ManagerDetail();
        }

        return user;
    }

    /**
     * 获取用户ID
     */
    public static Integer getManagerId() {
        return getManager().getId();
    }

    /**
     * 实时获取当前用户所属部门ID（从ManagerDetail直接获取，不缓存）
     */
    public static Integer getDeptId() {
        // 从SecurityContext中获取当前用户信息（ManagerDetail）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof ManagerDetail) {
            // 关键：从ManagerDetail中提取departId
            return ((ManagerDetail) principal).getDepartId();
        }
        return null;
    }

    /**
     * 实时获取当前用户姓名（使用真实姓名）
     */
    public static String getManagerName() {
        return getManager().getRealName();
    }

}