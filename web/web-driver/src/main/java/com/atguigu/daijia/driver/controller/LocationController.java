package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping(value="/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {

    @Autowired
    private LocationService locationService;

     /*
      * @Title: updateDriverLocation
      * @Author: pyzxW
      * @Date: 2025-03-23 19:40:19
      * @Params:
      * @Return: null
      * @Description: 开启接单服务
      */
    @Operation(summary = "开启接单服务：更新司机经纬度位置")
    @GuiguLogin
    @PostMapping("/updateDriverLocation")
    public Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm) {
        Long driverId = AuthContextHolder.getUserId();//司机id
        updateDriverLocationForm.setDriverId(driverId);
        return Result.ok(locationService.updateDriverLocation(updateDriverLocationForm));
    }

     /*
      * @Title: updateOrderLocationToCache
      * @Author: pyzxW
      * @Date: 2025-04-13 15:56:53
      * @Params:
      * @Return: null
      * @Description: 司机前往代驾地点之更新位置到redis
      */
    @Operation(summary = "司机赶往代驾起始点：更新订单位置到Redis缓存")
    @GuiguLogin
    @PostMapping("/updateOrderLocationToCache")
    public Result updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm) {
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
    }

     /*
      * @Title: saveOrderServiceLocation
      * @Author: pyzxW
      * @Date: 2025-04-18 17:23:44
      * @Params:  
      * @Return: null
      * @Description: 开始代驾服务
      */
    @Operation(summary = "开始代驾服务：保存代驾服务订单位置")
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return Result.ok(locationService.saveOrderServiceLocation(orderLocationServiceFormList));
    }
}

