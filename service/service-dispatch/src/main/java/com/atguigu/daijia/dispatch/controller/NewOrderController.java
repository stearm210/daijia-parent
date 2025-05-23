package com.atguigu.daijia.dispatch.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "司机新订单接口管理")
@RestController
@RequestMapping("/dispatch/newOrder")
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderController {

    @Autowired
    private NewOrderService newOrderService;

    //创建并启动任务调度方法
     /*
      * @Title: addAndStartTask
      * @Author: pyzxW
      * @Date: 2025-03-29 20:50:10
      * @Params: [newOrderTaskVo]
      * @Return: Result<Long>
      * @Description: 创建并启动任务调度方法
      */
    @Operation(summary = "添加并开始新订单任务调度")
    @PostMapping("/addAndStartTask")
    public Result<Long> addAndStartTask(@RequestBody NewOrderTaskVo newOrderTaskVo) {
        Long id = newOrderService.addAndStartTask(newOrderTaskVo);
        return Result.ok(id);
    }

     /*
      * @Title: findNewOrderQueueData
      * @Author: pyzxW
      * @Date: 2025-03-30 19:39:46
      * @Params:
      * @Return: null
      * @Description: 查询司机新订单数据
      */
    @Operation(summary = "查询司机新订单数据")
    @GetMapping("/findNewOrderQueueData/{driverId}")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable Long driverId) {
        return Result.ok(newOrderService.findNewOrderQueueData(driverId));
    }

     /*
      * @Title: clearNewOrderQueueData
      * @Author: pyzxW
      * @Date: 2025-03-30 19:40:17
      * @Params:
      * @Return: null
      * @Description: 如果订单已经被司机接收，则清空redis队列
      */
    @Operation(summary = "清空新订单队列数据")
    @GetMapping("/clearNewOrderQueueData/{driverId}")
    public Result<Boolean> clearNewOrderQueueData(@PathVariable Long driverId) {
        return Result.ok(newOrderService.clearNewOrderQueueData(driverId));
    }
}

