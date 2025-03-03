-- 1. 参数列表
-- 1.1 优惠价id
local voucherId = ARGV[1]
--1.2 用户id
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]

-- 2. 数据key
-- 2.1 库存key拼接
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单id
local orderKey = 'seckill:order:' .. voucherId

-- 3. 脚本业务
-- 3.1 判断库存是否充足 redis.call 出来的是 String 用 tonumber 转数字
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end
-- 3.2 判断用户是否下单
-- if (redis.call('sismember', orderKey, userId) == 1) then
--     -- 3.3 存在 返回2
--     return 2
-- end
-- 3.4 扣库存
redis.call('incrby', stockKey, -1)
-- 3.5 下单（保存用户）
redis.call('sadd', orderKey, userId)
-- 3.6 发送消息进消息队列中
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)

return 0