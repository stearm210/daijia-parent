package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {

    @Autowired
    private DriverInfoService driverInfoService;

     /*
      * @Title: login
      * @Author: pyzxW
      * @Date: 2025-03-09 16:15:05
      * @Params:
      * @Return: null
      * @Description: 小程序之登录操作
      */
    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable String code) {
        //一般来说是进行返回用户ID操作
        return Result.ok(driverInfoService.login(code));
    }

     /*
      * @Title: getDriverInfo
      * @Author: pyzxW
      * @Date: 2025-03-10 14:34:33
      * @Params:
      * @Return: null
      * @Description: 获取司机登录信息
      */
    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo/{driverId}")
    public Result<DriverLoginVo> getDriverInfo(@PathVariable Long driverId) {
        //根据司机的ID进行查询操作
        DriverLoginVo driverLoginVo = driverInfoService.getDriverInfo(driverId);
        return Result.ok(driverLoginVo);
    }

    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo/{driverId}")
    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId){
        DriverAuthInfoVo driverAuthInfoVo = driverInfoService.getDriverAuthInfo(driverId);
        return Result.ok(driverAuthInfoVo);
    }
//    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId) {
//        DriverAuthInfoVo driverAuthInfoVo = driverInfoService.getDriverAuthInfo(driverId);
//        return Result.ok(driverAuthInfoVo);
//    }

    //更新司机认证信息
     /*
      * @Title: updateDriverAuthInfo
      * @Author: pyzxW
      * @Date: 2025-03-16 15:57:24
      * @Params: [updateDriverAuthInfoForm]
      * @Return: Result<Boolean>
      * @Description: 更新司机认证信息
      */
    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm){
        Boolean isSuccess = driverInfoService.updateDriverAuthInfo(updateDriverAuthInfoForm);
        return Result.ok(isSuccess);
    }

//    @Operation(summary = "更新司机认证信息")
//    @PostMapping("/updateDriverAuthInfo")
//    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
//        Boolean isSuccess = driverInfoService.updateDriverAuthInfo(updateDriverAuthInfoForm);
//        return Result.ok(isSuccess);
//    }

    //创建司机人脸模型
     /*
      * @Title: creatDriverFaceModel
      * @Author: pyzxW
      * @Date: 2025-03-17 15:29:07
      * @Params: [driverFaceModelForm]
      * @Return: Result<Boolean>
      * @Description: 创建司机人脸模型
      */
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm){
        Boolean isSuccess = driverInfoService.creatDriverFaceModel(driverFaceModelForm);
        return Result.ok(isSuccess);
    }
//    @Operation(summary = "创建司机人脸模型")
//    @PostMapping("/creatDriverFaceModel")
//    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
//        Boolean isSuccess = driverInfoService.creatDriverFaceModel(driverFaceModelForm);
//        return Result.ok(isSuccess);
//    }

     /*
      * @Title: getDriverSet
      * @Author: pyzxW
      * @Date: 2025-03-23 19:50:51
      * @Params:
      * @Return: null
      * @Description: 获取司机的个性化设置信息
      */
    @Operation(summary = "获取司机设置信息")
    @GetMapping("/getDriverSet/{driverId}")
    public Result<DriverSet> getDriverSet(@PathVariable Long driverId) {
        return Result.ok(driverInfoService.getDriverSet(driverId));
    }

     /*
      * @Title: isFaceRecognition
      * @Author: pyzxW
      * @Date: 2025-03-31 14:38:18
      * @Params:  
      * @Return: null
      * @Description: 判断司机当日是否进行人脸识别
      */
    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GetMapping("/isFaceRecognition/{driverId}")
    Result<Boolean> isFaceRecognition(@PathVariable("driverId") Long driverId) {
        return Result.ok(driverInfoService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        return Result.ok(driverInfoService.verifyDriverFace(driverFaceModelForm));
    }

    //更新接单状态
    @Operation(summary = "更新接单状态")
    @GetMapping("/updateServiceStatus/{driverId}/{status}")
    public Result<Boolean> updateServiceStatus(@PathVariable Long driverId, @PathVariable Integer status) {
        return Result.ok(driverInfoService.updateServiceStatus(driverId, status));
    }


    @Operation(summary = "获取司机基本信息")
    @GetMapping("/getDriverInfo/{driverId}")
    public Result<DriverInfoVo> getDriverInfoOrder(@PathVariable Long driverId) {
        return Result.ok(driverInfoService.getDriverInfoOrder(driverId));
    }

    @Operation(summary = "获取司机OpenId")
    @GetMapping("/getDriverOpenId/{driverId}")
    public Result<String> getDriverOpenId(@PathVariable Long driverId) {
        return Result.ok(driverInfoService.getDriverOpenId(driverId));
    }
}

