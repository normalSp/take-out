package com.plumsnow.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.utils.RedisConstants;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.VoucherOrderDTO;
import com.plumsnow.dto.VoucherPageQueryDTO;
import com.plumsnow.entity.SeckillVoucher;
import com.plumsnow.entity.VoucherOrder;
import com.plumsnow.result.PageResult;
import com.plumsnow.result.Result;
import com.plumsnow.entity.Voucher;
import com.plumsnow.service.SeckillVoucherService;
import com.plumsnow.service.VoucherOrderService;
import com.plumsnow.service.VoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@Slf4j
@RestController
@Controller("userVoucherController")
@RequestMapping("/user/voucher")
@Api(tags = "C端-优惠卷相关接口")
public class VoucherController {

    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Resource
    private VoucherService voucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private VoucherOrderService voucherOrderService;

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    @ApiOperation("查询店铺的优惠券列表")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }


    /**
     * 分页查询店铺的优惠券列表
     * @param voucherPageQueryDTO
     * @return 优惠券列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询店铺的优惠券列表")
    public Result<PageResult> pageQueryVoucherOfShop(VoucherPageQueryDTO voucherPageQueryDTO) {
        //分页查询
        //仅查询当前商家的消费卷
        Long currentShopId = voucherPageQueryDTO.getShopId();
        log.info("分页查询店铺 {} 的优惠卷", voucherPageQueryDTO.getShopId());

        Page page = new Page(voucherPageQueryDTO.getPage(), voucherPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Voucher> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(null == currentShopId){
            return Result.error(MessageConstant.UNKNOWN_SHOPID);
        }
        lambdaQueryWrapper.eq(Voucher::getShopId, currentShopId);

        if(null != voucherPageQueryDTO.getType()){
            lambdaQueryWrapper.eq(Voucher::getType, voucherPageQueryDTO.getType());
        }

        if(null != voucherPageQueryDTO.getStatus()){
            lambdaQueryWrapper.eq(Voucher::getStatus, voucherPageQueryDTO.getStatus());
        }
        lambdaQueryWrapper.orderByDesc(Voucher::getUpdateTime);

        voucherService.page(page, lambdaQueryWrapper);

        return Result.success(new PageResult(page.getTotal(), page.getRecords()));

    }

    /**
     * 根据id查询秒杀优惠券
     * @param voucherId 优惠卷id
     * @return 秒杀优惠券信息
     */
    @GetMapping("/seckill/{voucherId}")
    @ApiOperation("查询店铺的秒杀优惠券")
    public Result<SeckillVoucher> querySeckillVoucherById(@PathVariable Long voucherId) {
        return Result.success(seckillVoucherService.getById(voucherId));
    }


    /**
     * 根据userId查询用户下单的优惠券
     * @param shopId
     * @return 优惠券信息
     */
    @GetMapping("/order/list/{shopId}")
    @ApiOperation("根据userId查询用户下单的优惠券")
    public Result<List<VoucherOrderDTO>> queryMyVoucher(@PathVariable Long shopId) {
/*        LambdaQueryWrapper<VoucherOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VoucherOrder::getUserId, BaseContext.getCurrentId());
        lambdaQueryWrapper.eq(VoucherOrder::getStatus, 1);
        lambdaQueryWrapper.orderByDesc(VoucherOrder::getUpdateTime);
        List<VoucherOrder> voucherOrderList = voucherOrderService.list(lambdaQueryWrapper);

        List<VoucherOrder> voucherOrderListToRemove = new ArrayList<>();

        for(VoucherOrder voucherOrder : voucherOrderList){
            log.info("voucherOrder.getVoucherId():{}", voucherOrder.getVoucherId());
            Voucher voucher = voucherService.getById(voucherOrder.getVoucherId());
            if(null == voucher){
                log.info("空:{}", voucherOrder.getVoucherId());
                continue;
            }
            if(!Objects.equals(voucher.getShopId(), shopId)){
                //voucherOrderList.remove(voucherOrder);
                voucherOrderListToRemove.add(voucherOrder);
            }
        }
        voucherOrderList.removeAll(voucherOrderListToRemove);

        List<VoucherOrderDTO> voucherOrderDTOList = new ArrayList<>();

        for(VoucherOrder voucherOrder : voucherOrderList){
            Voucher voucher = voucherService.getById(voucherOrder.getVoucherId());
            VoucherOrderDTO voucherOrderDTO = new VoucherOrderDTO();
            voucherOrderDTO.setShopId(voucher.getShopId());
            voucherOrderDTO.setPayValue(voucher.getPayValue());
            voucherOrderDTO.setActualValue(voucher.getActualValue());
            voucherOrderDTO.setType(voucher.getType());
            voucherOrderDTO.setPayStatus(voucherOrder.getStatus());
            voucherOrderDTO.setTitle(voucher.getTitle());
            voucherOrderDTO.setSubTitle(voucher.getSubTitle());
            voucherOrderDTO.setRules(voucher.getRules());
            voucherOrderDTO.setVoucherId(voucherOrder.getVoucherId());
            voucherOrderDTOList.add(voucherOrderDTO);
        }*/

/*      List<VoucherOrderDTO> voucherOrderDTOList = new ArrayList<>();*/
/*        //根据shopId查找所属的全部voucherId
        List<String> voucherIdList = stringRedisTemplate.opsForList().range(RedisConstants.SHOP_SECKILL_INDEX + shopId, 0, -1);

        //填充
        LambdaQueryWrapper<VoucherOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(VoucherOrder::getVoucherId, voucherIdList);
        lambdaQueryWrapper.eq(VoucherOrder::getUserId, BaseContext.getCurrentId());
        List<VoucherOrder> voucherOrderList = voucherOrderService.list(lambdaQueryWrapper);
        for(VoucherOrder voucherOrder : voucherOrderList){
            Voucher voucher = voucherService.getById(voucherOrder.getVoucherId());
            VoucherOrderDTO voucherOrderDTO = new VoucherOrderDTO();
            voucherOrderDTO.setShopId(voucher.getShopId());
            voucherOrderDTO.setPayValue(voucher.getPayValue());
            voucherOrderDTO.setActualValue(voucher.getActualValue());
            voucherOrderDTO.setType(voucher.getType());
            voucherOrderDTO.setPayStatus(voucherOrder.getStatus());
            voucherOrderDTO.setTitle(voucher.getTitle());
            voucherOrderDTO.setSubTitle(voucher.getSubTitle());
            voucherOrderDTO.setRules(voucher.getRules());
            voucherOrderDTO.setVoucherId(voucherOrder.getVoucherId());
            voucherOrderDTOList.add(voucherOrderDTO);
        }*/
        List<VoucherOrderDTO> voucherOrderDTOList = getVoucherOrders(shopId);
        return Result.success(voucherOrderDTOList);
    }

    //填充代码
    private VoucherOrderDTO convertToDTO(VoucherOrder order, Voucher voucher) {
        return VoucherOrderDTO.builder()
                .shopId(voucher.getShopId())
                .payValue(voucher.getPayValue())
                .actualValue(voucher.getActualValue())
                .type(voucher.getType())
                .payStatus(order.getStatus())
                .title(voucher.getTitle())
                .subTitle(voucher.getSubTitle())
                .rules(voucher.getRules())
                .voucherId(order.getVoucherId())
                .build();
    }

    public List<VoucherOrderDTO> getVoucherOrders(Long shopId) {
        // 1. 获取并转换优惠券ID列表（空值保护）
        List<Long> voucherIds = Optional.ofNullable(
                        stringRedisTemplate.opsForList().range(RedisConstants.SHOP_SECKILL_INDEX + shopId, 0, -1))
                .orElse(Collections.emptyList())
                .stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        if (voucherIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查询订单和优惠券信息（解决N+1问题）
        List<VoucherOrder> orders = voucherOrderService.lambdaQuery()
                .in(VoucherOrder::getVoucherId, voucherIds)
                .eq(VoucherOrder::getUserId, BaseContext.getCurrentId())
                .list();

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 批量获取优惠券数据
        Map<Long, Voucher> voucherMap = voucherService.listByIds(
                        orders.stream()
                                .map(VoucherOrder::getVoucherId)
                                .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Voucher::getId, Function.identity()));

        // 4. 使用Stream转换DTO
        return orders.stream()
                .map(order -> {
                    Voucher voucher = voucherMap.get(order.getVoucherId());
                    return voucher != null ? convertToDTO(order, voucher) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
