package com.plumsnow.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.VoucherOrderDTO;
import com.plumsnow.dto.VoucherPageQueryDTO;
import com.plumsnow.entity.SeckillVoucher;
import com.plumsnow.entity.VoucherOrder;
import com.plumsnow.result.PageResult;
import com.plumsnow.result.Result;
import com.plumsnow.entity.Voucher;
import com.plumsnow.service.ISeckillVoucherService;
import com.plumsnow.service.IVoucherOrderService;
import com.plumsnow.service.IVoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private IVoucherService voucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IVoucherOrderService voucherOrderService;

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
    @GetMapping("/voucher/order/list/{shopId}")
    @ApiOperation("根据userId查询用户下单的优惠券")
    public Result<List<VoucherOrderDTO>> queryMyVoucher(@PathVariable Long shopId) {
        LambdaQueryWrapper<VoucherOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VoucherOrder::getUserId, BaseContext.getCurrentId());
        lambdaQueryWrapper.eq(VoucherOrder::getStatus, 1);
        lambdaQueryWrapper.orderByDesc(VoucherOrder::getUpdateTime);
        List<VoucherOrder> voucherOrderList = voucherOrderService.list(lambdaQueryWrapper);

        for(VoucherOrder voucherOrder : voucherOrderList){
            Voucher voucher = voucherService.getById(voucherOrder.getVoucherId());
            if(!Objects.equals(voucher.getShopId(), shopId)){
                voucherOrderList.remove(voucherOrder);
            }
        }

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
            voucherOrderDTOList.add(voucherOrderDTO);
        }

        return Result.success(voucherOrderDTOList);
    }
}
