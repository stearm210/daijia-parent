package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    @Autowired
    private OrderService orderService;

     /*
      * @Title: getOrderStatus
      * @Author: pyzxW
      * @Date: 2025-03-22 20:00:47
      * @Params:
      * @Return: null
      * @Description: 查询订单状态
      */
    @Operation(summary = "查询订单状态")
    @GuiguLogin
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

     /*
      * @Title: findNewOrderQueueData
      * @Author: pyzxW
      * @Date: 2025-03-30 20:10:39
      * @Params:
      * @Return: null
      * @Description: 查询司机新订单数据
      */
    @Operation(summary = "查询司机新订单数据")
    @GuiguLogin
    @GetMapping("/findNewOrderQueueData")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.findNewOrderQueueData(driverId));
    }

    @Operation(summary = "司机抢单")
    @GuiguLogin
    @GetMapping("/robNewOrder/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.robNewOrder(driverId, orderId));
    }

     /*
      * @Title: searchDriverCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-03-31 14:25:39
      * @Params:
      * @Return: null
      * @Description: 查找司机当前之订单
      */
    @Operation(summary = "司机端查找当前订单")
    @GuiguLogin
    @GetMapping("/searchDriverCurrentOrder")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchDriverCurrentOrder(driverId));
    }

     /*
      * @Title: getOrderInfo
      * @Author: pyzxW
      * @Date: 2025-04-12 16:09:55
      * @Params:
      * @Return: null
      * @Description: 获取订单账单详细信息
      */
    @Operation(summary = "获取订单账单详细信息")
    @GuiguLogin
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, driverId));
    }

     /*
      * @Title: calculateDrivingLine
      * @Author: pyzxW
      * @Date: 2025-04-12 16:42:45
      * @Params:
      * @Return: null
      * @Description: 计算最佳驾驶线路 司机端司乘同显
      */
    @Operation(summary = "计算最佳驾驶线路")
    @GuiguLogin
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "司机到达代驾起始地点")
    @GuiguLogin
    @GetMapping("/driverArriveStartLocation/{orderId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.driverArriveStartLocation(orderId, driverId));
    }

     /*
      * @Title: updateOrderCart
      * @Author: pyzxW
      * @Date: 2025-04-13 17:07:24
      * @Params:
      * @Return: null
      * @Description: 更新代驾车辆信息
      */
    @Operation(summary = "更新代驾车辆信息")
    @GuiguLogin
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
        Long driverId = AuthContextHolder.getUserId();
        updateOrderCartForm.setDriverId(driverId);
        return Result.ok(orderService.updateOrderCart(updateOrderCartForm));
    }

     /*
      * @Title: startDrive
      * @Author: pyzxW
      * @Date: 2025-04-18 16:57:40
      * @Params:
      * @Return: null
      * @Description: 开始代驾服务
      */
    @Operation(summary = "开始代驾服务")
    @GuiguLogin
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm) {
        Long driverId = AuthContextHolder.getUserId();
        startDriveForm.setDriverId(driverId);
        return Result.ok(orderService.startDrive(startDriveForm));
    }

     /*
      * @Title: endDrive
      * @Author: pyzxW
      * @Date: 2025-04-22 15:24:37
      * @Params:  
      * @Return: null
      * @Description: 结束代驾之更新订单账单
      */
    @Operation(summary = "结束代驾服务更新订单账单")
    @GuiguLogin
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody OrderFeeForm orderFeeForm) {
        Long driverId = AuthContextHolder.getUserId();
        orderFeeForm.setDriverId(driverId);
        return Result.ok(orderService.endDrive(orderFeeForm));
    }

    @Operation(summary = "获取司机订单分页列表")
    @GuiguLogin
    @GetMapping("findDriverOrderPage/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {
        Long driverId = AuthContextHolder.getUserId();
        PageVo pageVo = orderService.findDriverOrderPage(driverId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "司机发送账单信息")
    @GuiguLogin
    @GetMapping("/sendOrderBillInfo/{orderId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.sendOrderBillInfo(orderId, driverId));
    }
}

