package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

     /*
      * @Title: searchCustomerCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-03-18 15:11:38
      * @Params:
      * @Return: null
      * @Description: 乘客端订单操作
      */

     //
     @Operation(summary = "乘客端查找当前订单")
     @GuiguLogin
     @GetMapping("/searchCustomerCurrentOrder")
     public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
         Long customerId = AuthContextHolder.getUserId();
         return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
     }

//    @Operation(summary = "乘客端查找当前订单")
//    @GuiguLogin
//    @GetMapping("/searchCustomerCurrentOrder")
//    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
//        Long customerId = AuthContextHolder.getUserId();
//        return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
//    }

    @Autowired
    private OrderService orderService;

      /*
       * @Title: expectOrder
       * @Author: pyzxW
       * @Date: 2025-03-21 21:03:00
       * @Params:
       * @Return: null
       * @Description: 预估订单数据
       */
    @Operation(summary = "预估订单数据")
    @GuiguLogin
    @PostMapping("/expectOrder")
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
        return Result.ok(orderService.expectOrder(expectOrderForm));
    }

     /*
      * @Title: submitOrder
      * @Author: pyzxW
      * @Date: 2025-03-22 19:38:39
      * @Params:
      * @Return: null
      * @Description: 乘客下单
      */
    @Operation(summary = "乘客下单")
    @GuiguLogin
    @PostMapping("/submitOrder")
    public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm) {
        submitOrderForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }

     /*
      * @Title: getOrderStatus
      * @Author: pyzxW
      * @Date: 2025-03-22 19:58:24
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

    @Operation(summary = "获取订单信息")
    @GuiguLogin
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, customerId));
    }

     /*
      * @Title: getDriverInfo
      * @Author: pyzxW
      * @Date: 2025-04-13 16:13:41
      * @Params:
      * @Return: null
      * @Description: 乘客查看司机的驾龄
      */
    @Operation(summary = "根据订单id获取司机基本信息")
    @GuiguLogin
    @GetMapping("/getDriverInfo/{orderId}")
    public Result<DriverInfoVo> getDriverInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getDriverInfo(orderId, customerId));
    }

     /*
      * @Title: getOrderLocation
      * @Author: pyzxW
      * @Date: 2025-04-13 16:23:44
      * @Params:
      * @Return: null
      * @Description: 乘客端查看对应的经纬度信息
      */
    @Operation(summary = "司机赶往代驾起始点：获取订单经纬度位置")
    @GuiguLogin
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getOrderLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getCacheOrderLocation(orderId));
    }

     /*
      * @Title: calculateDrivingLine
      * @Author: pyzxW
      * @Date: 2025-04-13 16:28:48
      * @Params:  
      * @Return: null
      * @Description: 乘客端显示司机之位置,计算最佳驾驶线路
      */
    @Operation(summary = "计算最佳驾驶线路")
    @GuiguLogin
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

     /*
      * @Title: getOrderServiceLastLocation
      * @Author: pyzxW
      * @Date: 2025-04-19 15:30:04
      * @Params:
      * @Return: null
      * @Description: 获取订单服务最后一个位置信息
      */
     @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
     @GuiguLogin
     @GetMapping("/getOrderServiceLastLocation/{orderId}")
     public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
         return Result.ok(orderService.getOrderServiceLastLocation(orderId));
     }

     /*
      * @Title: findCustomerOrderPage
      * @Author: pyzxW
      * @Date: 2025-04-25 15:38:28
      * @Params:  
      * @Return: null
      * @Description: 查询之订单分页信息
      */
     @Operation(summary = "获取乘客订单分页列表")
     @GuiguLogin
     @GetMapping("findCustomerOrderPage/{page}/{limit}")
     public Result<PageVo> findCustomerOrderPage(
             @Parameter(name = "page", description = "当前页码", required = true)
             @PathVariable Long page,

             @Parameter(name = "limit", description = "每页记录数", required = true)
             @PathVariable Long limit) {
         Long customerId = AuthContextHolder.getUserId();
         PageVo pageVo = orderService.findCustomerOrderPage(customerId, page, limit);
         return Result.ok(pageVo);
     }

    @Operation(summary = "创建微信支付")
    @GuiguLogin
    @PostMapping("/createWxPayment")
    public Result<WxPrepayVo> createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm) {
        Long customerId = AuthContextHolder.getUserId();
        createWxPaymentForm.setCustomerId(customerId);
        return Result.ok(orderService.createWxPayment(createWxPaymentForm));
    }

    @Operation(summary = "支付状态查询")
    @GuiguLogin
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderService.queryPayStatus(orderNo));
    }
}

