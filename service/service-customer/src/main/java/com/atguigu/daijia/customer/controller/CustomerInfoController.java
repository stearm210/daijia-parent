package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customer/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoController {

	@Autowired
	private CustomerInfoService customerInfoService;


	 /*
	  * @Title: getCustomerLoginInfo
	  * @Author: pyzxW
	  * @Date: 2025-03-08 15:02:08
	  * @Params:
	  * @Return: null
	  * @Description: 获取用户对应的信息
	  */
	@Operation(summary = "获取客户登录信息")
	@GetMapping("/getCustomerLoginInfo/{customerId}")
	public Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable Long customerId) {
		CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);
		return Result.ok(customerLoginVo);
	}

	//微信小程序登录接口
	@Operation(summary = "小程序授权登录")
	@GetMapping("/login/{code}")
	//login传入临时的票据，最后返回openid(用户的id值)
	public Result<Long> login(@PathVariable String code) {
		return Result.ok(customerInfoService.login(code));
	}

	 /*
	  * @Title: updateWxPhoneNumber
	  * @Author: pyzxW
	  * @Date: 2025-03-09 15:10:25
	  * @Params:
	  * @Return: null
	  * @Description: 微信号码之更新，方便进行操作
	  */
	@Operation(summary = "更新客户微信手机号码")
	@PostMapping("/updateWxPhoneNumber")
	public Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
		return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneForm));
	}

	 /*
	  * @Title: getCustomerOpenId
	  * @Author: pyzxW
	  * @Date: 2025-05-12 15:47:20
	  * @Params:
	  * @Return: null
	  * @Description: 获取用户的openid
	  */
	@Operation(summary = "获取客户OpenId")
	@GetMapping("/getCustomerOpenId/{customerId}")
	public Result<String> getCustomerOpenId(@PathVariable Long customerId) {
		return Result.ok(customerInfoService.getCustomerOpenId(customerId));
	}
}

