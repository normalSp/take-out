package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.entity.SeckillVoucher;
import com.plumsnow.mapper.SeckillVoucherMapper;
import com.plumsnow.service.ISeckillVoucherService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
