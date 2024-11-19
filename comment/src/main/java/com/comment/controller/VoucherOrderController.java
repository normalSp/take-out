package com.comment.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comment.dto.Result;
import com.comment.entity.VoucherOrder;
import com.comment.service.ISeckillVoucherService;
import com.comment.service.IVoucherOrderService;
import com.comment.service.IVoucherService;
import com.comment.utils.RedisIdWorker;
import com.comment.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Autowired
    private IVoucherService iVoucherService;

    @Autowired
    private ISeckillVoucherService iSeckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private IVoucherOrderService voucherOrderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 用于存放下单信息的阻塞队列，如果队列中没有对象就会被阻塞，直到有对象可以取出
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    //线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    

    //注解意思是在类初始化完成后就执行下面的方法
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    //线程任务
    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while(true){
                try {
                    //1. 获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );

                    //2. 判断消息获取是否成功
                    if(list == null || list.isEmpty()){
                        continue;
                    }

                    //3. 获取成功，创建订单
                    MapRecord<String, Object, Object> entries = list.get(0);
                    Map<Object, Object> value = entries.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);

                    //4. ACK 确认 XACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", entries.getId());

                } catch (Exception e) {
                    log.info("处理订单异常：{}", e.getMessage());

                    //handlePendingList();
                }

            }
        }
        private void handlePendingList() {
            while (true){
                try {
                    //1. 获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS stream.orders 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );

                    //2. 判断消息获取是否成功
                    if(list == null || list.isEmpty()){
                        break;
                    }

                    //3. 获取成功，创建订单
                    MapRecord<String, Object, Object> entries = list.get(0);
                    Map<Object, Object> value = entries.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);

                    //4. ACK 确认 XACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", entries.getId());

                } catch (Exception e) {
                    log.info("处理pending-list异常：{}", e.getMessage());

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    private static final DefaultRedisScript<Long> SECKILL_LUA_SCRIPT;
    static {
        SECKILL_LUA_SCRIPT = new DefaultRedisScript<>();
        SECKILL_LUA_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_LUA_SCRIPT.setResultType(Long.class);
    }

    private VoucherOrderController proxy;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        //在主线程拿到代理对象
        proxy = (VoucherOrderController) AopContext.currentProxy();

        //1. 执行lua脚本
        long orderId = redisIdWorker.getId("order");

        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_LUA_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );

        //2. 判断lua脚本结果
        if(result == 1L){
            return Result.fail("秒杀失败，库存不足");
        }
        if(result == 2L){
            return Result.fail("秒杀失败，不可重复下单");
        }


        //3. 返回订单id
        return Result.ok(orderId);
    }

    @Transactional
    public void createVoucherOrder(long voucherId, long userId) {
            //一人一单
            LambdaQueryWrapper<VoucherOrder> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(VoucherOrder::getUserId, userId);

            List<VoucherOrder> voucherOrders = voucherOrderService.list(lambdaQueryWrapper1);

            if (!voucherOrders.isEmpty()) {
                log.info("用户已经下过单了");
                return;
            }


            // 扣库存
            boolean b = iSeckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();

            if (!b) {
                log.info("抢卷失败，库存不足");
                return;
            }

            VoucherOrder voucherOrder = new VoucherOrder();
            long orderId = redisIdWorker.getId("voucher_order");
            voucherOrder.setId(orderId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(userId);

            voucherOrderService.save(voucherOrder);

    }


    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        //因为现在是多线程，没办法从UserHolder拿id
        Long userId = voucherOrder.getUserId();

        RLock lock = redissonClient.getLock("lock:order:" + userId);

        if (!lock.tryLock()) {
            log.info("不允许重复下单");
            return;
        }
        try {
            //没办法在这里从AopContext.currentProxy()拿代理对象，因为这个方法是基于ThreadLocal做的
            //VoucherOrderController proxy = (VoucherOrderController) AopContext.currentProxy();
            proxy.createVoucherOrder(voucherOrder.getVoucherId(), userId);
        }catch (Exception e){
            log.info("抢卷失败，请稍后再试");
        }
        finally {
            lock.unlock();
        }
    }
}
