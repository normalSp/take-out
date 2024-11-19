package com.comment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.comment.dto.Result;
import com.comment.entity.Voucher;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
