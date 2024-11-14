package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 根据动态条件统计订单数量
     * @param map
     * @return
     */
    @Select("select count(id) from orders where order_time > #{begin} and order_time < #{end} and status = #{status}  AND shop_id = #{shopId}")
    Integer countByMap(Map map, Long shopId);

    @Select("select count(id) from orders where order_time > #{begin} and order_time < #{end} AND shop_id = #{shopId}")
    Integer countByMapWithoutStatus(Map map, Long shopId);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    @Select("select sum(amount) from orders where order_time > #{begin} and order_time < #{end} and status = #{status} AND shop_id = #{shopId}")
    Double sumByMap(Map map, Long shopId);
}
