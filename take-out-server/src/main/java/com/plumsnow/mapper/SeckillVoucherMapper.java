package com.plumsnow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plumsnow.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@Mapper
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

}
