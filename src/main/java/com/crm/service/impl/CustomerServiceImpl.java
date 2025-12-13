package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.CustomerConvert;
import com.crm.entity.Customer;
import com.crm.entity.Department;
import com.crm.entity.SysManager;
import com.crm.mapper.CustomerMapper;
import com.crm.query.CustomerQuery;
import com.crm.query.CustomerTrendQuery;
import com.crm.query.IdQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.CustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.utils.DateUtils;
import com.crm.utils.ExcelUtils;
import com.crm.vo.CustomerTrendVO;
import com.crm.vo.CustomerVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.crm.utils.DateUtils.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
@Slf4j
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {
    @Override
    public PageResult<CustomerVO> getPage(CustomerQuery query) {
        //1.声明分页参数
        Page<CustomerVO> page = new Page<>(query.getPage(), query.getLimit());
        MPJLambdaWrapper<Customer> wrapper = selection(query);
        Page<CustomerVO> result = baseMapper.selectJoinPage(page, CustomerVO.class, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public void exportCustomer(CustomerQuery query, HttpServletResponse HttpResponse) {
        MPJLambdaWrapper<Customer> wrapper = selection(query);
        List<Customer> list = baseMapper.selectJoinList(wrapper);
        ExcelUtils.writeExcel(HttpResponse, list, "客户信息", "客户信息", CustomerVO.class);
    }

    @Override
    public void saveOrUpdate(CustomerVO customerVO) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, customerVO.getPhone());
        if(customerVO.getId() == null){
            Customer customer = baseMapper.selectOne(wrapper);
            if(customer != null){
                throw new RuntimeException("该手机号客户已存在，请勿重复添加");
            }
            Customer convert = CustomerConvert.INSTANCE.convert(customerVO);
            Integer managerId = SecurityUser.getManagerId();
            convert.setCreaterId(managerId);
            convert.setOwnerId(managerId);
            baseMapper.insert(convert);
        }else {
            wrapper.ne(Customer::getId, customerVO.getId());
            Customer customer = baseMapper.selectOne(wrapper);
            if(customer != null){
                throw new RuntimeException("该手机号客户已存在，请勿重复添加");
            }
            Customer convert = CustomerConvert.INSTANCE.convert(customerVO);
            baseMapper.updateById(convert);
        }
    }

    @Override
    public void removeCustomer(List<Integer> ids) {
        removeByIds(ids);
    }

    @Override
    public void customerToPublicPool(IdQuery idQuery) {
        Customer customer = baseMapper.selectById(idQuery.getId());
        if(customer==null){
            throw new ServerException("客户不存在,无法转入公海");
        }
        customer.setIsPublic(1);
        customer.setOwnerId(null);
        baseMapper.updateById(customer);
    }

    @Override
    public void publicPoolToPrivate(IdQuery idQuery) {
        Customer customer = baseMapper.selectById(idQuery.getId());
        if(customer == null){
            throw new ServerException("客户不存在,无法转入公海");
        }
        customer.setIsPublic(0);
        Integer ownerId = SecurityUser.getManagerId();
        customer.setOwnerId(ownerId);
        baseMapper.updateById(customer);
    }

    private MPJLambdaWrapper<Customer> selection(CustomerQuery query) {
        MPJLambdaWrapper<Customer> wrapper = new MPJLambdaWrapper<>();
        //2.构建查询关系
        wrapper.selectAll(Customer.class)
                .selectAs("o", SysManager::getAccount, CustomerVO::getOwnerName)
                .selectAs("c", SysManager::getAccount, CustomerVO::getCreaterName)
                .leftJoin(SysManager.class, "o", SysManager::getId, Customer::getOwnerId)
                .leftJoin(SysManager.class, "c", SysManager::getId, Customer::getCreaterId)
                // 关联“部门”查询部门信息（通过所属员工的depart_id）
                .leftJoin(SysManager.class, "m", SysManager::getId, Customer::getOwnerId)   // 客户 → 所属员工（m）
                .leftJoin(Department.class, "d", Department::getId, SysManager::getDepartId) // 员工 → 部门（d）
                .selectAs("m.depart_id", CustomerVO::getDepartId)   // 部门ID（从员工表取）
                .selectAs("d.name", CustomerVO::getDeptName);       // 部门名称

        //3.构造搜索字段
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Customer::getName, query.getName());
        }

        if (StringUtils.isNotBlank(query.getPhone())) {
            wrapper.like(Customer::getPhone, query.getPhone());
        }

        if (query.getLevel() != null) {
            wrapper.eq(Customer::getLevel, query.getLevel());
        }

        if (query.getSource() != null) {
            wrapper.eq(Customer::getSource, query.getSource());
        }

        if (query.getFollowStatus() != null) {
            wrapper.eq(Customer::getFollowStatus, query.getFollowStatus());
        }

        if (query.getIsPublic() != null) {
            wrapper.eq(Customer::getIsPublic, query.getIsPublic());
        }


        // 数据权限：仅看当前销售所在部门及子部门
        Integer currentDeptId = SecurityUser.getDeptId();
        if (currentDeptId != null) {
            List<Integer> subDeptIds = baseMapper.selectSubDeptIds(currentDeptId); // 查询子部门ID列表
            subDeptIds.add(currentDeptId); // 包含当前部门
            wrapper.in("m.depart_id", subDeptIds);
        }


        //构建列表排序
        wrapper.orderByDesc(Customer::getCreateTime);
        return wrapper;
    }

    @Override
    public Map<String, List> getCustomerTrendData(CustomerTrendQuery query
    ) {
// 处理不同请求类型的时间
// x轴时间数据
        List<String> timeList = new ArrayList<>();
// 统计客户变化数据
        List<Integer> countList = new ArrayList<>();
        List<CustomerTrendVO> tradeStatistics;
        if ("day".equals(query.getTransactionType())) {
            LocalDateTime now = LocalDateTime.now();
            // 截断毫秒和纳秒部分 影响sql 查询结果
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
            timeList = DateUtils.getDatesInRange(query.getTimeRange().get(
                    0), query.getTimeRange().get(1));
            tradeStatistics = baseMapper.getTradeStatisticsByDay(query);
        }
// 匹配时间点查询到的数据，没有值的默认为0
        List<CustomerTrendVO> finalTradeStatistics = tradeStatistics;
        timeList.forEach(item -> {
            CustomerTrendVO statisticsVO = finalTradeStatistics.stream()
                    .filter(vo -> {
                        if ("day".equals(query.getTransactionType())) {
                            // ⽐较⼩时段
                            return item.substring(0, 2).equals(vo.getTradeTime().substring(0, 2));
                        } else {
                            return item.equals(vo.getTradeTime());
                        }
                    })
                    .findFirst()
                    .orElse(null); // 找不到则为 null
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

}
