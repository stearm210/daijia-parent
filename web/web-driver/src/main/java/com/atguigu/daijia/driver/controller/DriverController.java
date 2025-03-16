package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

     /*
      * @Title: login
      * @Author: pyzxW
      * @Date: 2025-03-09 20:16:29
      * @Params:
      * @Return: null
      * @Description: 司机登录操作
      */
    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code) {
        //返回的信息是token，可以用于后续的操作
        return Result.ok(driverService.login(code));
    }

     /*
      * @Title: getDriverLoginInfo
      * @Author: pyzxW
      * @Date: 2025-03-09 20:16:48
      * @Params:  
      * @Return: null
      * @Description: 获取对应的司机登录数据
      */
    @Operation(summary = "获取司机登录信息")
    @GuiguLogin
    @GetMapping("/getDriverLoginInfo")
    public Result<DriverLoginVo> getDriverLoginInfo() {
        //1 获取用户id, 线程中获取对应的id
        Long driverId = AuthContextHolder.getUserId();
        //2 远程调用获取司机信息
        Result<DriverLoginVo> loginVoResult = driverInfoFeignClient.getDriverLoginInfo(driverId);
        //从result中获得对应的driverLogVo
        DriverLoginVo driverLoginVo = loginVoResult.getData();
        return Result.ok(driverLoginVo);
    }

     /*
      * @Title: getDriverAuthInfo
      * @Author: pyzxW
      * @Date: 2025-03-16 15:30:26
      * @Params:
      * @Return: null
      * @Description: 获取司机之认证信息
      */
    @Operation(summary = "获取司机认证信息")
    @GuiguLogin
    @GetMapping("/getDriverAuthInfo")
    public Result<DriverAuthInfoVo> getDriverAuthInfo() {
        //获取登录用户id，当前是司机id
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverAuthInfo(driverId));
    }

     /*
      * @Title: updateDriverAuthInfo
      * @Author: pyzxW
      * @Date: 2025-03-16 16:13:58
      * @Params:
      * @Return: null
      * @Description: 更新操作
      */
    @Operation(summary = "更新司机认证信息")
    @GuiguLogin
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        //获取司机id, 线程中获取对应的id
        updateDriverAuthInfoForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }

    @Operation(summary = "创建司机人脸模型")
    @GuiguLogin
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.creatDriverFaceModel(driverFaceModelForm));
    }

    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GuiguLogin
    @GetMapping("/isFaceRecognition")
    Result<Boolean> isFaceRecognition() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @GuiguLogin
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "开始接单服务")
    @GuiguLogin
    @GetMapping("/startService")
    public Result<Boolean> startService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.startService(driverId));
    }

    @Operation(summary = "停止接单服务")
    @GuiguLogin
    @GetMapping("/stopService")
    public Result<Boolean> stopService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.stopService(driverId));
    }
}

