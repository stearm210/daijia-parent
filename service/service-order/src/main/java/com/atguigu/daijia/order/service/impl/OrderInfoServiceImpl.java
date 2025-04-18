package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.OrderBill;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderProfitsharing;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //乘客下单

//    @Override
//    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
//        OrderInfo orderInfo = new OrderInfo();
//        BeanUtils.copyProperties(orderInfoForm,orderInfo);
//        //订单号,生成唯一之订单号
//        String orderNo = UUID.randomUUID().toString().replaceAll("-","");
//        orderInfo.setOrderNo(orderNo);
//        //订单状态
//        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
//        orderInfoMapper.insert(orderInfo);
//
//        //日志记录，传入ID和状态
//        this.log(orderInfo.getId(),orderInfo.getStatus());
//        return orderInfo.getId();
//    }
/*
 * @Title: saveOrderInfo
 * @Author: pyzxW
 * @Date: 2025-03-22 19:30:21
 * @Params: [orderInfoForm]
 * @Return: Long
 * @Description: 乘客下单
 */
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        //order_info添加订单数据
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm,orderInfo);
        //订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-","");
        orderInfo.setOrderNo(orderNo);
        //订单状态
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfoMapper.insert(orderInfo);

        //生成订单之后，发送延迟消息
        this.sendDelayMessage(orderInfo.getId());

        //记录日志
        this.log(orderInfo.getId(),orderInfo.getStatus());

        //向redis添加标识
        //接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK,
                "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);

        return orderInfo.getId();
    }

    //生成订单之后，发送延迟消息
    private void sendDelayMessage(Long orderId) {
        try{
            //1 创建队列
            RBlockingQueue<Object> blockingDueue = redissonClient.getBlockingQueue("queue_cancel");

            //2 把创建队列放到延迟队列里面
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDueue);

            //3 发送消息到延迟队列里面
            //设置过期时间
            delayedQueue.offer(orderId.toString(),15,TimeUnit.MINUTES);

        }catch (Exception e) {
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

     /*
      * @Title: getOrderStatus
      * @Author: pyzxW
      * @Date: 2025-03-22 19:49:07
      * @Params:
      * @Return: null
      * @Description: 查询订单状态
      */
    //根据订单id获取订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        //sql语句： select status from order_info where id=?
        //数据库查询语句
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        //订单ID，比较是否相等
        wrapper.eq(OrderInfo::getId,orderId);
        //查值
        wrapper.select(OrderInfo::getStatus);
        //调用mapper方法
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //订单不存在
        if(orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }
        //返回对应状态
        return orderInfo.getStatus();
    }

     /*
      * @Title: robNewOrder
      * @Author: pyzxW
      * @Date: 2025-04-08 13:50:22
      * @Params:
      * @Return: null
      * @Description: 司机抢单服务
      */
    //Redisson分布式锁
    //司机抢单
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        //判断订单是否存在，通过Redis，减少数据库压力
        //之前保存订单时有在redis中对订单添加标识，方便判断是否有资格被抢单，同样也设置了过期时间
        //设置if判断，如果没有这个表示，则认为订单已经被抢了，则直接返回抢单失败
        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)){
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //使用分布式锁进行单的单独线程访问
        //创建锁
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            //获取锁
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME,TimeUnit.SECONDS);
            if (flag){
                //表示已经得到锁
                //防止重复抢单
                if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)){
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                //开始抢单
                //修改order_info中的值为2，表示已经接单。司机id、司机接单时间
                //修改条件：订单id+司机id
                LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(OrderInfo::getId,orderId);
                OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
                //设置orderinfo数据库中需要修改的值
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                //调用方法修改
                int rows = orderInfoMapper.updateById(orderInfo);
                //影响行数
                if (rows != 1){
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                //订单被抢到之后，则需要在redis数据库中删除抢单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        } catch (Exception e) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }finally {
            //释放
            if (lock.isLocked()){
                lock.unlock();
            }
        }
        return true;
    }
//    public Boolean robNewOrder(Long driverId, Long orderId) {
//        //判断订单是否存在，通过Redis，减少数据库压力
//        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
//            //抢单失败
//            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
//        }
//
//        //创建锁
//        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
//
//        try {
//            //获取锁
//            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
//            if(flag) {
//                if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
//                    //抢单失败
//                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
//                }
//                //司机抢单
//                //修改order_info表订单状态值2：已经接单 + 司机id + 司机接单时间
//                //修改条件：根据订单id
//                LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(OrderInfo::getId,orderId);
//                OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
//                //设置
//                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
//                orderInfo.setDriverId(driverId);
//                orderInfo.setAcceptTime(new Date());
//                //调用方法修改
//                int rows = orderInfoMapper.updateById(orderInfo);
//                if(rows != 1) {
//                    //抢单失败
//                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
//                }
//
//                //删除抢单标识
//                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
//            }
//        }catch (Exception e) {
//            //抢单失败
//            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
//        }finally {
//            //释放
//            if(lock.isLocked()) {
//                lock.unlock();
//            }
//        }
//        return true;
//    }

     /*
      * @Title: searchCustomerCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-04-12 15:08:04
      * @Params:
      * @Return: null
      * @Description: 乘客端查找当前的订单
      */
    //乘客端查找当前订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        //CurrentOrderInfoVo中有订单的对应信息，这些信息使用vo进行封装，同时使用订单id可以进行查询
        //封装条件
        //查询乘客id
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        //判等。
        wrapper.eq(OrderInfo::getCustomerId,customerId);

        //各种状态值
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        };
        wrapper.in(OrderInfo::getStatus,statusArray);

        //获取最新的一条记录
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");

        //调用方法
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo对象中
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null){
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            //这里表示当前没有订单
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }

        return currentOrderInfoVo;
    }
//    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
//        //封装条件
//        //乘客id
//        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderInfo::getCustomerId,customerId);
//
//        //各种状态
//        Integer[] statusArray = {
//                OrderStatus.ACCEPTED.getStatus(),
//                OrderStatus.DRIVER_ARRIVED.getStatus(),
//                OrderStatus.UPDATE_CART_INFO.getStatus(),
//                OrderStatus.START_SERVICE.getStatus(),
//                OrderStatus.END_SERVICE.getStatus(),
//                OrderStatus.UNPAID.getStatus()
//        };
//        wrapper.in(OrderInfo::getStatus,statusArray);
//
//        //获取最新一条记录
//        wrapper.orderByDesc(OrderInfo::getId);
//        wrapper.last(" limit 1");
//
//        //调用方法
//        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
//
//        //封装到CurrentOrderInfoVo
//        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
//        if(orderInfo != null) {
//            currentOrderInfoVo.setOrderId(orderInfo.getId());
//            currentOrderInfoVo.setStatus(orderInfo.getStatus());
//            currentOrderInfoVo.setIsHasCurrentOrder(true);
//        } else {
//            currentOrderInfoVo.setIsHasCurrentOrder(false);
//        }
//        return currentOrderInfoVo;
//    }

     /*
      * @Title: searchDriverCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-04-12 15:37:48
      * @Params:
      * @Return: null
      * @Description: 司机端查找当前订单
      */
    //司机端查找当前订单
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        //查询订单数据库，查找订单对应的司机id
        wrapper.eq(OrderInfo::getDriverId,driverId);
        //对应的状态
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        wrapper.in(OrderInfo::getStatus,statusArray);
        //降序排列
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");//获取最新的一条
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if(null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }


//    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
//        //封装条件
//        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderInfo::getDriverId,driverId);
//        Integer[] statusArray = {
//                OrderStatus.ACCEPTED.getStatus(),
//                OrderStatus.DRIVER_ARRIVED.getStatus(),
//                OrderStatus.UPDATE_CART_INFO.getStatus(),
//                OrderStatus.START_SERVICE.getStatus(),
//                OrderStatus.END_SERVICE.getStatus()
//        };
//        wrapper.in(OrderInfo::getStatus,statusArray);
//        wrapper.orderByDesc(OrderInfo::getId);
//        wrapper.last(" limit 1");
//        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
//        //封装到vo
//        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
//        if(null != orderInfo) {
//            currentOrderInfoVo.setStatus(orderInfo.getStatus());
//            currentOrderInfoVo.setOrderId(orderInfo.getId());
//            currentOrderInfoVo.setIsHasCurrentOrder(true);
//        } else {
//            currentOrderInfoVo.setIsHasCurrentOrder(false);
//        }
//        return currentOrderInfoVo;
//    }

     /*
      * @Title: driverArriveStartLocation
      * @Author: pyzxW
      * @Date: 2025-04-13 16:42:56
      * @Params:
      * @Return: null
      * @Description: 司机到达起始点
      */
    //司机到达起始点
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        //更新订单状态和到达时间，条件：
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        //查询订单与司机
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.eq(OrderInfo::getDriverId,driverId);
        OrderInfo orderInfo = new OrderInfo();
        /// 司机到达
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        //到达时间
        orderInfo.setArriveTime(new Date());
        int rows = orderInfoMapper.update(orderInfo,wrapper);
        //判断是否在数据库中影响
        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }
//    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
//        // 更新订单状态和到达时间，条件：orderId + driverId
//        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderInfo::getId,orderId);
//        wrapper.eq(OrderInfo::getDriverId,driverId);
//
//        OrderInfo orderInfo = new OrderInfo();
//        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
//        orderInfo.setArriveTime(new Date());
//
//        int rows = orderInfoMapper.update(orderInfo, wrapper);
//
//        if(rows == 1) {
//            return true;
//        } else {
//            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
//        }
//    }

     /*
      * @Title: updateOrderCart
      * @Author: pyzxW
      * @Date: 2025-04-13 16:53:28
      * @Params:
      * @Return: null
      * @Description: 更新车辆的信息
      */
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        //封装对应的条件，订单的id以及司机的id
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,updateOrderCartForm.getDriverId());

        //设置订单的状态
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm,orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());

        //update中的两个参数分别为 修改后的值  +  条件
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

     /*
      * @Title: startDriver
      * @Author: pyzxW
      * @Date: 2025-04-18 16:49:53
      * @Params:
      * @Return: null
      * @Description: 开始代驾服务
      */
    //开始代驾服务
    @Override
    public Boolean startDriver(StartDriveForm startDriveForm){
        //1. 根据订单id  +  司机id  更新订单状态  和 开
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        //根据两个id来确认订单
        wrapper.eq(OrderInfo::getId,startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,startDriveForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        //开始服务之状态值
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());

        //判断是否在数据库中进行更改
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if(rows == 1) {
            return true;
        } else {
            //失败则抛出自定义异常
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }
//    public Boolean startDriver(StartDriveForm startDriveForm) {
//        //根据订单id  +  司机id  更新订单状态  和 开始代驾时间
//        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderInfo::getId,startDriveForm.getOrderId());
//        wrapper.eq(OrderInfo::getDriverId,startDriveForm.getDriverId());
//
//        OrderInfo orderInfo = new OrderInfo();
//        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
//        orderInfo.setStartServiceTime(new Date());
//
//        int rows = orderInfoMapper.update(orderInfo, wrapper);
//        if(rows == 1) {
//            return true;
//        } else {
//            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
//        }
//    }

    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
       // 09 <= time < 10   <= time1  <    11
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OrderInfo::getStartServiceTime,startTime);
        wrapper.lt(OrderInfo::getStartServiceTime,endTime);
        Long count = orderInfoMapper.selectCount(wrapper);
        return count;
    }

    @Autowired
    private OrderBillMapper orderBillMapper;

    @Autowired
    private OrderProfitsharingMapper orderProfitsharingMapper;

    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        //1 更新订单信息
        // update order_info set ..... where id=? and driver_id=?
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,updateOrderBillForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if(rows == 1) {
            //添加账单数据
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm,orderBill);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);

            //添加分账信息
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            //TODO
            orderProfitsharing.setRuleId(new Date().getTime());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);

        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    //获取乘客订单分页列表
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectCustomerOrderPage(pageParam,customerId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderBill::getOrderId,orderId);
        OrderBill orderBill = orderBillMapper.selectOne(wrapper);

        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill,orderBillVo);
        return orderBillVo;
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderProfitsharing::getOrderId,orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing,orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        //更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo,customerId);
        if(orderPayVo != null) {
            String content = orderPayVo.getStartLocation() + " 到 "+orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        //1 根据订单编号查询，判断订单状态
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo,orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        if(orderInfo == null || orderInfo.getStatus() == OrderStatus.PAID.getStatus()) {
            return true;
        }

        //2 更新状态
        LambdaQueryWrapper<OrderInfo> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(OrderInfo::getOrderNo,orderNo);

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
        updateOrderInfo.setPayTime(new Date());

        int rows = orderInfoMapper.update(updateOrderInfo, updateWrapper);

        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        //根据订单编号查询订单表
        OrderInfo orderInfo =
                orderInfoMapper.selectOne(
                        new LambdaQueryWrapper<OrderInfo>()
                                .eq(OrderInfo::getOrderNo, orderNo)
                                .select(OrderInfo::getId,OrderInfo::getDriverId));

        //根据订单id查询系统奖励表
        OrderBill orderBill =
                orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
                        .eq(OrderBill::getOrderId, orderInfo.getId())
                        .select(OrderBill::getRewardFee));

        //封装到vo里面
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }

    //调用方法取消订单
    @Override
    public void orderCancel(long orderId) {
        //orderId查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //判断
        if(orderInfo.getStatus()==OrderStatus.WAITING_ACCEPT.getStatus()) {
            //修改订单状态：取消状态
            orderInfo.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
            int rows = orderInfoMapper.updateById(orderInfo);
            if(rows == 1) {
                //删除接单标识

                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        }
    }

    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        orderBillMapper.updateCouponAmount(orderId,couponAmount);
        return true;
    }


    public void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }


    //司机抢单：乐观锁方案解决并发问题
    public Boolean robNewOrder1(Long driverId, Long orderId) {
        //判断订单是否存在，通过Redis，减少数据库压力
        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //司机抢单
        //update order_info set status =2 ,driver_id = ?,accept_time = ?
        // where id=? and status = 1
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.eq(OrderInfo::getStatus,OrderStatus.WAITING_ACCEPT.getStatus());

        //修改值
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
        orderInfo.setDriverId(driverId);
        orderInfo.setAcceptTime(new Date());

        //调用方法修改
        int rows = orderInfoMapper.update(orderInfo,wrapper);
        if(rows != 1) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //删除抢单标识
        redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
        return true;
    }
}



