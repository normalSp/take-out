package com.plumsnow.controller.admin;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.VoucherPageQueryDTO;
import com.plumsnow.entity.SeckillVoucher;
import com.plumsnow.result.PageResult;
import com.plumsnow.result.Result;
import com.plumsnow.entity.Voucher;
import com.plumsnow.service.SeckillVoucherService;
import com.plumsnow.service.VoucherService;
import com.plumsnow.utils.RedisConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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
@Controller("adminVoucherController")
@RequestMapping("/admin/voucher")
@Api(tags = "优惠卷管理相关接口")
public class VoucherController {

    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Resource
    private VoucherService voucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增普通券
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    @PostMapping("/addVoucher")
    @ApiOperation("新增普通卷")
    public Result addVoucher(@RequestBody Voucher voucher) {
        Long currentShopId = BaseContext.getCurrentShopId();
        voucher.setShopId(currentShopId);

        voucherService.save(voucher);
        return Result.success(voucher.getId());
    }

    /**
     * 新增秒杀券
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("/addSeckill")
    @ApiOperation("新增秒杀券")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        Long currentShopId = BaseContext.getCurrentShopId();
        voucher.setShopId(currentShopId);

        voucherService.addSeckillVoucher(voucher);
        return Result.success(voucher.getId());
    }

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
    public Result pageQueryVoucherOfShop(VoucherPageQueryDTO voucherPageQueryDTO) {
        //分页查询
        //仅查询当前商家的消费卷
        Long currentShopId = BaseContext.getCurrentShopId();
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
    @ApiOperation("查询店铺的优惠券列表")
    public Result querySeckillVoucherById(@PathVariable Long voucherId) {
        return Result.success(seckillVoucherService.getById(voucherId));
    }


    /**
     * 更新优惠券
     * @param voucher
     * @return 优惠券id
     */
    @PostMapping("/update")
    @ApiOperation("更新优惠券")
    public Result updateVoucher(@RequestBody Voucher voucher) {
        //仅查询当前商家的消费卷
        Long currentShopId = BaseContext.getCurrentShopId();
        log.info("分页查询店铺 {} 的优惠卷", currentShopId);

        Voucher voucherBefore = voucherService.getById(voucher.getId());
        //判断Redis和数据库中的库存stock一样不一样
        if(voucherBefore.getType() == 1){
            String stock = stringRedisTemplate.opsForValue().get(RedisConstants.SECKILL_STOCK_KEY + voucher.getId());
            voucherBefore.setStock(seckillVoucherService.getById(voucher.getId()).getStock());
            if(!stock.equals(voucherBefore.getStock().toString())){
                return Result.error(MessageConstant.STOCK_NOT_SAME);
            }
        }

        if(voucherBefore.getType() == 0 && voucher.getType() == 0){
            voucherService.updateById(voucher);
        }else if (voucherBefore.getType() == 1 && voucher.getType() == 1){
            voucherService.updateById(voucher);

            LambdaUpdateWrapper<SeckillVoucher> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(SeckillVoucher::getVoucherId, voucher.getId());
            lambdaUpdateWrapper.set(SeckillVoucher::getStock, voucher.getStock());
            seckillVoucherService.update(lambdaUpdateWrapper);

            //修改缓存库存stock
            stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
        }else if (voucherBefore.getType() == 0 && voucher.getType() == 1){
            voucherService.updateById(voucher);

            // 保存秒杀信息
            SeckillVoucher seckillVoucher = new SeckillVoucher();
            seckillVoucher.setVoucherId(voucher.getId());
            seckillVoucher.setStock(voucher.getStock());
            seckillVoucher.setBeginTime(voucher.getBeginTime());
            seckillVoucher.setEndTime(voucher.getEndTime());
            seckillVoucherService.save(seckillVoucher);

            // 保存库存信息到redis
            stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
            stringRedisTemplate.opsForValue().getOperations().delete(RedisConstants.SECKILL_TYPE + voucher.getId());
        }else if (voucherBefore.getType() == 1 && voucher.getType() == 0){
            voucherService.updateById(voucher);

            LambdaQueryWrapper<SeckillVoucher> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SeckillVoucher::getVoucherId, voucher.getId());
            seckillVoucherService.remove(lambdaQueryWrapper);

            // 构建要删除的 Redis 键
            String key1 = RedisConstants.SECKILL_STOCK_KEY + voucher.getId();
            String key2 = RedisConstants.SECKILL_TYPE + voucher.getId();
            // 删除对应的键
            stringRedisTemplate.opsForValue().getOperations().delete(key1);
            stringRedisTemplate.opsForValue().getOperations().delete(key2);
        }

        return Result.success(voucher.getId());
    }

    /**
     * 删除优惠券
     * @param voucherId
     * @return
     */
    @DeleteMapping("/delete/{voucherId}")
    @ApiOperation("删除优惠券")
    public Result deleteVoucher(@PathVariable Long voucherId) {
        Voucher voucher = voucherService.getById(voucherId);
        if(voucher.getType() == 0){
            voucherService.removeById(voucherId);
        }else if (voucher.getType() == 1){
            voucherService.removeById(voucherId);
            LambdaQueryWrapper<SeckillVoucher> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SeckillVoucher::getVoucherId, voucherId);
            seckillVoucherService.remove(lambdaQueryWrapper);

            // 构建要删除的 Redis 键
            String key1 = RedisConstants.SECKILL_STOCK_KEY + voucher.getId();
            String key2 = RedisConstants.SECKILL_TYPE + voucher.getId();
            // 删除对应的键
            stringRedisTemplate.opsForValue().getOperations().delete(key1);
            stringRedisTemplate.opsForValue().getOperations().delete(key2);
        }

        return Result.success();
    }
}
