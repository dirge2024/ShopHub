package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service

public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    // Lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    // 存储订单的阻塞队列,参数为队列长度
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 执行任务的线程池, ctrl+shift+U 转换大写
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        // 启动一个线程，执行订单处理任务
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    public class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while(true)
            {//并不会堆cpu造成负担，因为下面有take方法，线程会阻塞在这里，等待队列中有订单信息时才会继续往下执行
                try{
                    // 从队列中获取订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 创建订单
                    handleVoucherOrder(voucherOrder);
                }catch (Exception e){
                    log.error("处理订单异常", e);
                }
            }
        }


    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 在Redis已经做了库存是否充足和一人一单的校验,能够到这里说明用户已经秒杀成功了,所以这里其实不需要加锁
        // 1.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")
                .eq("voucher_id", voucherOrder.getId())
                .gt("stock", 0)
                .update();
        if(!success){
            // 扣减库存失败
            log.error("库存不足");
            return;
        }
        // 2.创建订单
        save(voucherOrder);

    }


    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 执行lua搅拌，判断有没有资格下单
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        int r = result.intValue();
        if(r == 1)
        {
            // 代表没有购买资格
            return Result.fail("库存不足");
        }
        if(r == 2)
        {
            // 代表重复购买
            return Result.fail("用户已经购买过一次了");
        }
        Long orderId = redisIdWorker.nextID("order");
        // 2.保存信息到阻塞队列,会有一个线程不断从当中取出信息,执行扣库存和生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);    // 订单ID
        voucherOrder.setUserId(userId); // 用户ID
        voucherOrder.setVoucherId(voucherId); // 优惠券ID
        orderTasks.add(voucherOrder);
        return Result.ok(orderId);
    }


//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1、查询秒杀卷
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        //2、判断秒杀卷是否合法
//        //在当前之间之前还是之后
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now()))
//        {
//            return Result.fail("秒杀还未开始");
//        }
//        if(voucher.getEndTime().isBefore(LocalDateTime.now()))
//        {
//            return Result.fail("秒杀已经结束");
//        }
//
//        //3、判断库存是否充足
//        if(voucher.getStock()<1)
//        {
//            return Result.fail("秒杀卷以抢空");
//        }
//
//
//        //先去字符串常量池找字符串对象，使得加锁同一个对象
//        //先获取锁，再开启事务，事务结束后再释放锁，因为需要互斥但是锁的范围不能太大
//        Long userId = UserHolder.getUser().getId();
//        //intern()把字符放入常量池
//
//        //不使用synchronized锁，使用redis分布式锁
////        SimpleRedisLock lock = new SimpleRedisLock("order:"+userId,stringRedisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:"+userId);
//        boolean isLock = lock.tryLock();
//        if(!isLock)
//        {
//            return Result.fail("一个用户只允许下一单");
//        }
//        try {
//            //spring的事务是基于代理对象的，这里直接调用相当于ths.xxx,并非代理对象
//            //因此事务不会生效，所以要拿到代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            //释放锁
//            lock.unlock();
//        }
//    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //4、一人一单
        int count = query().eq("user_id",userId).eq("voucher_id",voucherId).count();
        if(count>0)
        {
            return Result.fail("用户已经购买过该优惠卷");
        }
        //5、扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")//set stock = stock -1
                .eq("voucher_id",voucherId).gt("stock",0)
                .update();                 //where id = ? && stock > 0
        if(!success)
        {
            throw new RuntimeException("秒杀卷扣减库存失败");
        }
        //6、创建订单，订单id用前面写好的分布式id
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextID("order");
        //订单id
        voucherOrder.setId(orderId);
        //用户id
        voucherOrder.setUserId((UserHolder.getUser().getId()));
        //代金卷id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //7、返回订单id

        return Result.ok(orderId);
    }

}

