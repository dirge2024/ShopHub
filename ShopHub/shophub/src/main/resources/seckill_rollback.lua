-- 优惠券id
local voucherId = ARGV[1];
-- 用户id
local userId = ARGV[2];

-- 库存key
local stockKey = 'seckill:stock:' .. voucherId;
-- 已下单用户集合key
local orderKey = 'seckill:order:' .. voucherId;

-- 只有确实写入过购买标记时才回滚，避免误加库存
if (redis.call('SISMEMBER', orderKey, userId) == 1) then
    redis.call('SREM', orderKey, userId);
    redis.call('INCRBY', stockKey, 1);
    return 1;
end

return 0;
