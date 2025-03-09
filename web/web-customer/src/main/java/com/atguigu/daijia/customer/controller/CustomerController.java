package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

//乘客接口开发
//乘客接口模块，乘客调用入口
@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {

    @Autowired
    private CustomerService customerInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;

//    @Operation(summary = "获取客户登录信息")
//    @GetMapping("/getCustomerLoginInfo")
//    public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
//        //1.从请求头中获取token字符串
//
//        //2.根据token查询redis
//        //3.查询token在redis里面对应用户id
//        String customerId = (String)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
//        //判断操作,id是否存在？
//        if (!StringUtils.hasText(customerId)){
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        //4.根据用户id进行远程调用 得到用户信息
//        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(Long.parseLong(customerId));
//        //判断service的返回码code
//        Integer code = customerLoginVoResult.getCode();
//        //如果code不为200则调用失败
//        if(code != 200) {
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        //判断用户信息是否为空
//        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
//        if (customerLoginVo == null){
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        //5.返回用户信息
//        return Result.ok(customerLoginVo);
//    }

    @Operation(summary = "获取客户登录信息")
    //已经调用了自定义注解方法，因此可以使用q直接获取对应的id操作
    @GuiguLogin
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo() {

        //1 从ThreadLocal获取用户id
        Long customerId = AuthContextHolder.getUserId();

        //调用service
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);

        //返回用户信息
        return Result.ok(customerLoginVo);
    }

//    @Operation(summary = "获取客户登录信息")
//    @GetMapping("/getCustomerLoginInfo")
//    public Result<CustomerLoginVo>
//                    getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
//
//        //1 从请求头获取token字符串
////        HttpServletRequest request
////        String token = request.getHeader("token");
//
//        //调用service
//        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerLoginInfo(token);
//
//        return Result.ok(customerLoginVo);
//    }

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerInfoService.login(code));
    }

     /*
      * @Title: updateWxPhone
      * @Author: pyzxW
      * @Date: 2025-03-09 15:20:12
      * @Params:
      * @Return: null
      * @Description: 更新用户手机号
      */
    @Operation(summary = "更新用户微信手机号")
    @GuiguLogin
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneForm));
    }
}

