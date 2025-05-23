package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.coupon.client.CouponFeignClient;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.map.client.WxPayFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    @Autowired
    private MapFeignClient mapFeignClient;

    @Autowired
    private FeeRuleFeignClient feeRuleFeignClient;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private NewOrderFeignClient newOrderFeignClient;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;

     /*
      * @Title: expectOrder
      * @Author: pyzxW
      * @Date: 2025-03-21 21:04:04
      * @Params:
      * @Return: null
      * @Description: 预估订单数据
      */
    //预估订单数据
    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
        //获取驾驶线路
        //获取当前经纬度
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(expectOrderForm,calculateDrivingLineForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        //获取订单费用
        FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
        //设置订单对应的距离、代驾时间、等候时间
        calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
        calculateOrderFeeForm.setStartTime(new Date());
        calculateOrderFeeForm.setWaitMinute(0);
        //远程调用rule规则，获取最终的结果
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        //封装ExpectOrderVo
        //得到对应的路线以及费用
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        expectOrderVo.setDrivingLineVo(drivingLineVo);
        expectOrderVo.setFeeRuleResponseVo(feeRuleResponseVo);
        return expectOrderVo;
    }

    // //乘客下单
     /*
      * @Title: submitOrder
      * @Author: pyzxW
      * @Date: 2025-03-22 19:39:14
      * @Params: [submitOrderForm]
      * @Return: Long
      * @Description: 乘客下单
      */
    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {
        //1 重新计算驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(submitOrderForm,calculateDrivingLineForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        //2 重新订单费用
        FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
        calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
        calculateOrderFeeForm.setStartTime(new Date());
        calculateOrderFeeForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        //封装数据
        //订单信息
        OrderInfoForm orderInfoForm = new OrderInfoForm();
        BeanUtils.copyProperties(submitOrderForm,orderInfoForm);
        //设置距离
        orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
        //设置金额
        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
        Result<Long> orderInfoResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
        //获取订单ID
        Long orderId = orderInfoResult.getData();

        //任务调度：查询附近可以接单司机
        NewOrderTaskVo newOrderDispatchVo = new NewOrderTaskVo();
        newOrderDispatchVo.setOrderId(orderId);
        newOrderDispatchVo.setStartLocation(orderInfoForm.getStartLocation());
        newOrderDispatchVo.setStartPointLongitude(orderInfoForm.getStartPointLongitude());
        newOrderDispatchVo.setStartPointLatitude(orderInfoForm.getStartPointLatitude());
        newOrderDispatchVo.setEndLocation(orderInfoForm.getEndLocation());
        newOrderDispatchVo.setEndPointLongitude(orderInfoForm.getEndPointLongitude());
        newOrderDispatchVo.setEndPointLatitude(orderInfoForm.getEndPointLatitude());
        newOrderDispatchVo.setExpectAmount(orderInfoForm.getExpectAmount());
        newOrderDispatchVo.setExpectDistance(orderInfoForm.getExpectDistance());
        newOrderDispatchVo.setExpectTime(drivingLineVo.getDuration());
        newOrderDispatchVo.setFavourFee(orderInfoForm.getFavourFee());
        newOrderDispatchVo.setCreateTime(new Date());
        //远程调用,添加任务
        Long jobId = newOrderFeignClient.addAndStartTask(newOrderDispatchVo).getData();
        //返回订单id
        return orderId;
    }

     /*
      * @Title: getOrderStatus
      * @Author: pyzxW
      * @Date: 2025-03-22 19:59:03
      * @Params:
      * @Return: null
      * @Description: 查询订单状态
      */
    //查询订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> integerResult = orderInfoFeignClient.getOrderStatus(orderId);
        return integerResult.getData();
    }

     /*
      * @Title: searchCustomerCurrentOrder
      * @Author: pyzxW
      * @Date: 2025-04-12 15:33:18
      * @Params:
      * @Return: null
      * @Description: 乘客查找当前订单
      */
    //乘客查找当前订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        return orderInfoFeignClient.searchCustomerCurrentOrder(customerId).getData();
    }

     /*
      * @Title: getOrderInfo
      * @Author: pyzxW
      * @Date: 2025-04-12 16:26:56
      * @Params:
      * @Return: null
      * @Description: 根据订单id获取订单信息
      * 司机发送对应的账单信息给乘客，乘客查看之后进行微信支付
      */
    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long customerId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        //判断
        if(orderInfo.getCustomerId() != customerId) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //获取司机信息
        DriverInfoVo driverInfoVo = null;
        Long driverId = orderInfo.getDriverId();
        if(driverId != null) {
            driverInfoVo = driverInfoFeignClient.getDriverInfo(driverId).getData();
        }

        //获取账单信息
        OrderBillVo orderBillVo = null;
        //如果是属于代付款状态则执行
        if(orderInfo.getStatus() >= OrderStatus.UNPAID.getStatus()) {
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        }

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo,orderInfoVo);

        //设置账单信息
        orderInfoVo.setOrderBillVo(orderBillVo);
        orderInfoVo.setDriverInfoVo(driverInfoVo);
        return orderInfoVo;
    }

     /*
      * @Title: getDriverInfo
      * @Author: pyzxW
      * @Date: 2025-04-13 16:14:33
      * @Params:
      * @Return: null
      * @Description: 根据订单的id查询司机的基本信息
      */
    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        //根据订单id获取订单信息
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if(orderInfo.getCustomerId() != customerId) {
            //判断是否是当前乘客之订单
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //远程调用司机信息接口
        return driverInfoFeignClient.getDriverInfo(orderInfo.getDriverId()).getData();
    }
//    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
//        //根据订单id获取订单信息
//        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
//        if(orderInfo.getCustomerId() != customerId) {
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        return driverInfoFeignClient.getDriverInfo(orderInfo.getDriverId()).getData();
//    }

     /*
      * @Title: getCacheOrderLocation
      * @Author: pyzxW
      * @Date: 2025-04-13 16:24:07
      * @Params:
      * @Return: null
      * @Description: 乘客端查看对应的经纬度信息
      */
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return locationFeignClient.getCacheOrderLocation(orderId).getData();
    }

     /*
      * @Title: calculateDrivingLine
      * @Author: pyzxW
      * @Date: 2025-04-13 16:29:28
      * @Params:
      * @Return: null
      * @Description: 乘客端显示司机之位置,计算最佳驾驶线路
      */
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

     /*
      * @Title: getOrderServiceLastLocation
      * @Author: pyzxW
      * @Date: 2025-04-19 15:30:19
      * @Params:
      * @Return: null
      * @Description: 获取订单服务最后一个位置信息
      */
     @Override
     public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
         return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
     }

    @Override
    public PageVo findCustomerOrderPage(Long customerId, Long page, Long limit) {
        return orderInfoFeignClient.findCustomerOrderPage(customerId,page,limit).getData();
    }

    @Autowired
    private WxPayFeignClient wxPayFeignClient;

    @Autowired
    private CouponFeignClient couponFeignClient;

    @Override
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        //获取订单支付信息
        OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(),
                createWxPaymentForm.getCustomerId()).getData();
        //判断
        if(orderPayVo.getStatus() != OrderStatus.UNPAID.getStatus()) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //获取乘客和司机openid
        String customerOpenId = customerInfoFeignClient.getCustomerOpenId(orderPayVo.getCustomerId()).getData();

        String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();

        //处理优惠卷
        BigDecimal couponAmount = null;
        //判断
        if (null == orderPayVo.getCouponAmount()
                && null != createWxPaymentForm.getCustomerCouponId()
                && createWxPaymentForm.getCustomerCouponId() != 0) {
            UseCouponForm useCouponForm = new UseCouponForm();
            useCouponForm.setOrderId(orderPayVo.getOrderId());
            useCouponForm.setCustomerCouponId(createWxPaymentForm.getCustomerCouponId());
            useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
            useCouponForm.setCustomerId(createWxPaymentForm.getCustomerId());
            couponAmount = couponFeignClient.useCoupon(useCouponForm).getData();
        }

        //更新订单支付金额
        //获取支付金额
        BigDecimal payAmount = orderPayVo.getPayAmount();
        if(couponAmount != null) {
            orderInfoFeignClient.updateCouponAmount(orderPayVo.getOrderId(),couponAmount).getData();

            //当前支付金额
            payAmount = payAmount.subtract(couponAmount);
        }

        //封装需要数据到实体类，远程调用发起微信支付
        PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
        paymentInfoForm.setCustomerOpenId(customerOpenId);
        paymentInfoForm.setDriverOpenId(driverOpenId);
        paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());

        paymentInfoForm.setAmount(payAmount);

        paymentInfoForm.setContent(orderPayVo.getContent());
        paymentInfoForm.setPayWay(1);

        WxPrepayVo wxPrepayVo = wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
        return wxPrepayVo;
    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        return wxPayFeignClient.queryPayStatus(orderNo).getData();
    }
}
