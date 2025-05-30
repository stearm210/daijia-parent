package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {

    @Autowired
    private OrderJobMapper orderJobMapper;

    @Autowired
    private XxlJobClient xxlJobClient;

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //创建并启动任务调度方法
     /*
      * @Title: addAndStartTask
      * @Author: pyzxW
      * @Date: 2025-03-29 20:50:53
      * @Params: [newOrderTaskVo] 
      * @Return: Long
      * @Description: 模拟调度中心，创建并启动任务调度方法
      */
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        //1.判断当前订单是否启动任务调度
        //根据订单id查询
        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
        //数据库中对比对应的信息
        wrapper.eq(OrderJob::getOrderId,newOrderTaskVo.getOrderId());
        //查到对应id的信息时则会返回一个对象，一个id对应一个任务
        OrderJob orderJob = orderJobMapper.selectOne(wrapper);

        //2.没有启动任务调度则直接进行操作
        //模拟调度中心的操作
        if(orderJob == null) {
            //创建并启动任务调度
            //String executorHandler 执行任务job方法
            // String param
            // String corn 执行cron表达式
            // String desc 描述信息
            //获得对应的jobid值
            //每一分钟执行这个任务
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "",
                    "0 0/1 * * * ?",
                    "新创建订单任务调度：" + newOrderTaskVo.getOrderId());

            //记录任务调度信息
            orderJob = new OrderJob();
            //任务id
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            //工作id
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            //插入数据库
            orderJobMapper.insert(orderJob);
        }
        return orderJob.getJobId();
    }

//    @Override
//    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
//        //1 判断当前订单是否启动任务调度
//        //根据订单id查询
//        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderJob::getOrderId,newOrderTaskVo.getOrderId());
//        OrderJob orderJob = orderJobMapper.selectOne(wrapper);
//
//        //2 没有启动，进行操作
//        if(orderJob == null) {
//            //创建并启动任务调度
//            //String executorHandler 执行任务job方法
//            // String param
//            // String corn 执行cron表达式
//            // String desc 描述信息
//            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "",
//                    "0 0/1 * * * ?",
//                    "新创建订单任务调度：" + newOrderTaskVo.getOrderId());
//
//            //记录任务调度信息
//            orderJob = new OrderJob();
//            orderJob.setOrderId(newOrderTaskVo.getOrderId());
//            orderJob.setJobId(jobId);
//            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
//            orderJobMapper.insert(orderJob);
//        }
//        return orderJob.getJobId();
//    }

    /*
    * 只有在订单被指派任务之后，才能搜索附近的代驾司机
    * */
    //执行任务：搜索附近代驾司机
    @Override
    public void executeTask(long jobId) {
        //1.根据jobid查询数据库，当前任务是否已经创建
        //如果没有创建，不往下执行了
        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderJob::getJobId,jobId);
        OrderJob orderJob = orderJobMapper.selectOne(wrapper);
        if (orderJob == null){
            //没有创建，不往下执行了
            return;
        }

        //2.查询订单状态，如果当前订单接单状态，继续执行。如果当前订单不是接单状态，停止任务调度
       //获取OrderJob里面的对象
        //得到参数信息，当前订单的状态？
        String jsonString = orderJob.getParameter();
        //根据状态得到对应的vo
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(jsonString, NewOrderTaskVo.class);

        //获取订单id
        Long orderId = newOrderTaskVo.getOrderId();
        //得到对应的订单状态
        Integer status = orderInfoFeignClient.getOrderStatus(orderId).getData();
        if (status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            //停止任务调度
            xxlJobClient.stopJob(jobId);
            return;
        }

        //3.远程调用：搜索附近满足条件可以接单的司机
        //4.远程调用之后，获取满足可以接单的司机集合
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        //远程调用，得到满足条件的司机列表
        List<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();

        //5.遍历司机集合，得到每个司机，为每个司机，创建一个临时队列，存储新订单信息
        nearByDriverVoList.forEach(driver -> {
            //把订单信息推送给满足条件的多个司机
            //使用redis的set类型
            //根据订单id生成key
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();
            //记录司机的id，防止重复计数
            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
            //如果没有被推送？
            if (!isMember){
                //把订单信息推送给满足条件的多个司机
                redisTemplate.opsForSet().add(repeatKey,driver.getDriverId());
                //过期时间:15分钟，超过15分钟没有接单自动取消
                redisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);

                //一个司机会面对多个订单，所以需要一个临时队列，存储订单信息
                //向这个vo对象中传入对应的参数信息
                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(driver.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());

                //新订单保存到司机的临时队列中去，在redis中使用list集合完成
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driver.getDriverId();
                //redis中放入对象数据
                redisTemplate.opsForList().leftPush(key,JSONObject.toJSONString(newOrderDataVo));
                //设置过期时间1分钟
                redisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
            }
        });
    }

    /*
     * @Title: findNewOrderQueueData
     * @Author: pyzxW
     * @Date: 2025-03-30 19:43:08
     * @Params:
     * @Return: null
     * @Description: 获取最新订单
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();
        //生成key
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driverId;
        //在redis中查询数据，获取司机临时队列中的那些订单
        Long size = redisTemplate.opsForList().size(key);
        if (size > 0){
            for (int i = 0; i < size; i++){
                String content = (String) redisTemplate.opsForList().leftPop(key);
                //得到的订单内容装换为vo类型，方便操作
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }


//    @Override
//    public void executeTask(long jobId) {
//        //1 根据jobid查询数据库，当前任务是否已经创建
//        //如果没有创建，不往下执行了
//        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderJob::getJobId,jobId);
//        OrderJob orderJob = orderJobMapper.selectOne(wrapper);
//        if(orderJob == null) {
//            //不往下执行了
//            return;
//        }
//
//        //2 查询订单状态，如果当前订单接单状态，继续执行。如果当前订单不是接单状态，停止任务调度
//        //获取OrderJob里面对象
//        String jsonString = orderJob.getParameter();
//        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(jsonString, NewOrderTaskVo.class);
//
//        //获取orderId
//        Long orderId = newOrderTaskVo.getOrderId();
//        Integer status = orderInfoFeignClient.getOrderStatus(orderId).getData();
//        if(status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
//            //停止任务调度
//            xxlJobClient.stopJob(jobId);
//            return;
//        }
//
//        //3 远程调用:搜索附近满足条件可以接单司机
//        //4 远程调用之后，获取满足可以接单司机集合
//        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
//        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
//        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
//        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
//        //远程调用
//        List<NearByDriverVo> nearByDriverVoList =
//                locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();
//
//        //5 遍历司机集合，得到每个司机，为每个司机创建临时队列，存储新订单信息
//        nearByDriverVoList.forEach(driver -> {
//            //使用Redis的set类型
//            //根据订单id生成key
//            String repeatKey =
//                    RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();
//            //记录司机id，防止重复推送
//            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
//            if(!isMember) {
//                //把订单信息推送给满足条件多个司机
//                redisTemplate.opsForSet().add(repeatKey,driver.getDriverId());
//                //过期时间：15分钟，超过15分钟没有接单自动取消
//                redisTemplate.expire(repeatKey,
//                        RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME,
//                        TimeUnit.MINUTES);
//
//                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
//                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
//                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
//                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
//                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
//                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
//                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
//                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
//                newOrderDataVo.setDistance(driver.getDistance());
//                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());
//                //新订单保存司机的临时队列，Redis里面List集合
//                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driver.getDriverId();
//                redisTemplate.opsForList().leftPush(key,JSONObject.toJSONString(newOrderDataVo));
//                //过期时间：1分钟
//                redisTemplate.expire(key,1, TimeUnit.MINUTES);
//            }
//        });
//    }



//    @Override
//    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
//        List<NewOrderDataVo> list = new ArrayList<>();
//        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
//        long size = redisTemplate.opsForList().size(key);
//        if(size > 0) {
//            for(int i=0; i<size; i++) {
//                String content = (String)redisTemplate.opsForList().leftPop(key);
//                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
//                list.add(newOrderDataVo);
//            }
//        }
//        return list;
//        List<NewOrderDataVo> list = new ArrayList<>();
//        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
//        Long size = redisTemplate.opsForList().size(key);
//        if(size > 0) {
//            for (int i = 0; i < size; i++) {
//                String content = (String)redisTemplate.opsForList().leftPop(key);
//                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content,NewOrderDataVo.class);
//                list.add(newOrderDataVo);
//            }
//        }
//        return list;
//    }

    //清空队列数据
    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(key);
        return true;
    }
}
