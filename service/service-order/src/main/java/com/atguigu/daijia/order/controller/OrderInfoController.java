package com.atguigu.daijia.order.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@Tag(name = "订单API接口管理")
@RestController
@RequestMapping(value="/order/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoController {

    @Autowired
    private OrderInfoService orderInfoService;

     /*
      * @Title: saveOrderInfo
      * @Author: pyzxW
      * @Date: 2025-03-22 19:28:25
      * @Params:
      * @Return: null
      * @Description: 保存订单信息
      */
    @Operation(summary = "保存订单信息")
    @PostMapping("/saveOrderInfo")
    public Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm) {
        return Result.ok(orderInfoService.saveOrderInfo(orderInfoForm));
    }
//    @Operation(summary = "保存订单信息")
//    @PostMapping("/saveOrderInfo")
//    public Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm) {
//        return Result.ok(orderInfoService.saveOrderInfo(orderInfoForm));
//    }

     /*
      * @Title: getOrderStatus
      * @Author: pyzxW
      * @Date: 2025-03-22 19:48:23
      * @Params:
      * @Return: null
      * @Description: 查询订单状态
      */
    @Operation(summary = "根据订单id获取订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderStatus(orderId));
    }

     /*
      * @Title: robNewOrder
      * @Author: pyzxW
      * @Date: 2025-04-08 13:49:08
      * @Params:
      * @Return: null
      * @Description: 司机抢单接口
      */
    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long driverId, @PathVariable Long orderId) {
        return Result.ok(orderInfoService.robNewOrder(driverId, orderId));
    }


     /*
      * @Title: searchCustomerCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-04-12 15:06:46
      * @Params:
      * @Return: null
      * @Description: 乘客端查找当前订单
      */
    @Operation(summary = "乘客端查找当前订单")
    @GetMapping("/searchCustomerCurrentOrder/{customerId}")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable Long customerId) {
        return Result.ok(orderInfoService.searchCustomerCurrentOrder(customerId));
    }

     /*
      * @Title: searchDriverCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-04-12 15:35:12
      * @Params:
      * @Return: null
      * @Description: 司机端查找当前订单
      */
    @Operation(summary = "司机端查找当前订单")
    @GetMapping("/searchDriverCurrentOrder/{driverId}")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable Long driverId) {
        return Result.ok(orderInfoService.searchDriverCurrentOrder(driverId));
    }

     /*
      * @Title: getOrderInfo
      * @Author: pyzxW
      * @Date: 2025-04-12 16:04:12
      * @Params:
      * @Return: null
      * @Description: 根据订单id获取订单信息
      */
    @Operation(summary = "根据订单id获取订单信息")
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfo> getOrderInfo(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getById(orderId));
    }

     /*
      * @Title: driverArriveStartLocation
      * @Author: pyzxW
      * @Date: 2025-04-13 16:42:17
      * @Params:
      * @Return: null
      * @Description: 司机到达起始点
      */
    @Operation(summary = "司机到达起始点")
    @GetMapping("/driverArriveStartLocation/{orderId}/{driverId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId, @PathVariable Long driverId) {
        return Result.ok(orderInfoService.driverArriveStartLocation(orderId, driverId));
    }

     /*
      * @Title: updateOrderCart
      * @Author: pyzxW
      * @Date: 2025-04-13 16:53:02
      * @Params:
      * @Return: null
      * @Description: 更新车辆的信息
      */
    @Operation(summary = "更新代驾车辆信息")
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
        return Result.ok(orderInfoService.updateOrderCart(updateOrderCartForm));
    }

    //开始代驾服务
     /*
      * @Title: startDriver
      * @Author: pyzxW
      * @Date: 2025-04-18 16:49:27
      * @Params: [startDriveForm]
      * @Return: Result<Boolean>
      * @Description: 开始代驾服务
      */
    @PostMapping("/startDrive")
    public Result<Boolean> startDriver(@RequestBody StartDriveForm startDriveForm) {
        Boolean flag = orderInfoService.startDriver(startDriveForm);
        return Result.ok(flag);
    }
//    @PostMapping("/startDrive")
//    public Result<Boolean> startDriver(@RequestBody StartDriveForm startDriveForm) {
//        Boolean flag = orderInfoService.startDriver(startDriveForm);
//        return Result.ok(flag);
//    }

     /*
      * @Title: getOrderNumByTime
      * @Author: pyzxW
      * @Date: 2025-04-21 16:55:48
      * @Params:
      * @Return: null
      * @Description: 订单之数量查询
      */
    @Operation(summary = "根据时间段获取订单数")
    @GetMapping("/getOrderNumByTime/{startTime}/{endTime}")
    public Result<Long> getOrderNumByTime(@PathVariable String startTime, @PathVariable String endTime) {
        return Result.ok(orderInfoService.getOrderNumByTime(startTime, endTime));
    }

     /*
      * @Title: endDrive
      * @Author: pyzxW
      * @Date: 2025-04-22 14:53:22
      * @Params:
      * @Return: null
      * @Description: 结束代驾服务之更新订单操作
      */
    @Operation(summary = "结束代驾服务更新订单账单")
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody UpdateOrderBillForm updateOrderBillForm) {
        return Result.ok(orderInfoService.endDrive(updateOrderBillForm));
    }

     /*
      * @Title: findCustomerOrderPage
      * @Author: pyzxW
      * @Date: 2025-04-25 15:14:13
      * @Params:
      * @Return: null
      * @Description: 获取乘客订单分页列表
      */
     @Operation(summary = "获取乘客订单分页列表")
     @GetMapping("/findCustomerOrderPage/{customerId}/{page}/{limit}")
     public Result<PageVo> findCustomerOrderPage(@PathVariable Long customerId, @PathVariable Long page, @PathVariable Long limit){
         //创建page对象
         Page<OrderInfo> pageParam = new Page<>(page,limit);
         //调用service方法实现分页条件查询
         PageVo pageVo = orderInfoService.findCustomerOrderPage(pageParam,customerId);
         pageVo.setPage(page);
         pageVo.setLimit(limit);
         return Result.ok(pageVo);
     }

     /*
      * @Title: findDriverOrderPage
      * @Author: pyzxW
      * @Date: 2025-04-25 15:43:32
      * @Params:
      * @Return: null
      * @Description: 获取司机订单分页列表
      */
    @Operation(summary = "获取司机订单分页列表")
    @GetMapping("/findDriverOrderPage/{driverId}/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(
            @Parameter(name = "driverId", description = "司机id", required = true)
            @PathVariable Long driverId,

            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        PageVo pageVo = orderInfoService.findDriverOrderPage(pageParam, driverId);
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

     /*
      * @Title: getOrderBillInfo
      * @Author: pyzxW
      * @Date: 2025-05-08 20:39:13
      * @Params:
      * @Return: null
      * @Description: 获取对应账单信息
      */
    @Operation(summary = "根据订单id获取实际账单信息")
    @GetMapping("/getOrderBillInfo/{orderId}")
    public Result<OrderBillVo> getOrderBillInfo(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderBillInfo(orderId));
    }

    @Operation(summary = "根据订单id获取实际分账信息")
    @GetMapping("/getOrderProfitsharing/{orderId}")
    public Result<OrderProfitsharingVo> getOrderProfitsharing(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderProfitsharing(orderId));
    }

     /*
      * @Title: sendOrderBillInfo
      * @Author: pyzxW
      * @Date: 2025-05-09 19:15:06
      * @Params:  
      * @Return: null
      * @Description: 发送账单信息
      */
    @Operation(summary = "发送账单信息")
    @GetMapping("/sendOrderBillInfo/{orderId}/{driverId}")
    Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId, @PathVariable Long driverId) {
        return Result.ok(orderInfoService.sendOrderBillInfo(orderId, driverId));
    }

     /*
      * @Title: getOrderPayVo
      * @Author: pyzxW
      * @Date: 2025-05-12 15:59:31
      * @Params:
      * @Return: null
      * @Description: 获取订单的支付信息
      */
    @Operation(summary = "获取订单支付信息")
    @GetMapping("/getOrderPayVo/{orderNo}/{customerId}")
    public Result<OrderPayVo> getOrderPayVo(@PathVariable String orderNo, @PathVariable Long customerId) {
        return Result.ok(orderInfoService.getOrderPayVo(orderNo, customerId));
    }

    @Operation(summary = "更改订单支付状态")
    @GetMapping("/updateOrderPayStatus/{orderNo}")
    public Result<Boolean> updateOrderPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderInfoService.updateOrderPayStatus(orderNo));
    }

    @Operation(summary = "获取订单的系统奖励")
    @GetMapping("/getOrderRewardFee/{orderNo}")
    public Result<OrderRewardVo> getOrderRewardFee(@PathVariable String orderNo) {
        return Result.ok(orderInfoService.getOrderRewardFee(orderNo));
    }

    @Operation(summary = "更新订单优惠券金额")
    @GetMapping("/updateCouponAmount/{orderId}/{couponAmount}")
    public Result<Boolean> updateCouponAmount(@PathVariable Long orderId, @PathVariable BigDecimal couponAmount) {
        return Result.ok(orderInfoService.updateCouponAmount(orderId, couponAmount));
    }
}



