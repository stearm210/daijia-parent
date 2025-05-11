package com.atguigu.daijia.map.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/*
* 封装位置相关的接口
* */

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping("/map/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {

    @Autowired
    private LocationService locationService;

     /*
      * @Title: updateDriverLocation
      * @Author: pyzxW
      * @Date: 2025-03-22 20:27:10
      * @Params:
      * @Return: null
      * @Description: 司机开启接单，更新司机位置信息
      */
    //司机开启接单，更新司机位置信息
    @Operation(summary = "开启接单服务：更新司机经纬度位置")
    @PostMapping("/updateDriverLocation")
    public Result<Boolean> updateDriverLocation(@RequestBody
                                                    UpdateDriverLocationForm updateDriverLocationForm) {
        Boolean flag = locationService.updateDriverLocation(updateDriverLocationForm);
        return Result.ok(flag);
    }

     /*
      * @Title: removeDriverLocation
      * @Author: pyzxW
      * @Date: 2025-03-22 20:27:19
      * @Params:
      * @Return: null
      * @Description: 司机关闭接单，删除司机位置信息
      */
    //司机关闭接单，删除司机位置信息
    @Operation(summary = "关闭接单服务：删除司机经纬度位置")
    @DeleteMapping("/removeDriverLocation/{driverId}")
    public Result<Boolean> removeDriverLocation(@PathVariable Long driverId) {
        return Result.ok(locationService.removeDriverLocation(driverId));
    }

     /*
      * @Title: searchNearByDriver
      * @Author: pyzxW
      * @Date: 2025-03-23 20:28:20
      * @Params:
      * @Return: null
      * @Description: 搜索附近满足条件的司机
      */
    @Operation(summary = "搜索附近满足条件的司机")
    @PostMapping("/searchNearByDriver")
    public Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody
                                                               SearchNearByDriverForm searchNearByDriverForm) {
        return Result.ok(locationService.searchNearByDriver(searchNearByDriverForm));
    }

     /*
      * @Title: updateOrderLocationToCache
      * @Author: pyzxW
      * @Date: 2025-04-13 15:46:06
      * @Params:
      * @Return: null
      * @Description: 司机代驾之位置缓存
      */
    @Operation(summary = "司机赶往代驾起始点：更新订单地址到缓存")
    @PostMapping("/updateOrderLocationToCache")
    public Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm) {
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
    }

     /*
      * @Title: getCacheOrderLocation
      * @Author: pyzxW
      * @Date: 2025-04-13 16:20:23
      * @Params:
      * @Return: null
      * @Description: 司机赶往代驾起始点：乘客获取订单经纬度位置
      */
    @Operation(summary = "司机赶往代驾起始点：获取订单经纬度位置")
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getCacheOrderLocation(@PathVariable Long orderId) {
        return Result.ok(locationService.getCacheOrderLocation(orderId));
    }

    //批量保存代驾服务订单位置
     /*
      * @Title: saveOrderServiceLocation
      * @Author: pyzxW
      * @Date: 2025-04-18 17:09:53
      * @Params: [orderLocationServiceFormList]
      * @Return: Result<Boolean>
      * @Description: 批量保存代驾服务订单位置
      */
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return Result.ok(locationService.saveOrderServiceLocation(orderLocationServiceFormList));
    }

     /*
      * @Title: getOrderServiceLastLocation
      * @Author: pyzxW
      * @Date: 2025-04-19 15:19:38
      * @Params:
      * @Return: null
      * @Description: 获取司机的最后位置信息
      */
     @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
     @GetMapping("/getOrderServiceLastLocation/{orderId}")
     public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
         return Result.ok(locationService.getOrderServiceLastLocation(orderId));
     }

     /*
      * @Title: calculateOrderRealDistance
      * @Author: pyzxW
      * @Date: 2025-04-21 15:25:06
      * @Params:
      * @Return: null
      * @Description: 计算实际的里程数
      */
    @Operation(summary = "代驾服务：计算订单实际里程")
    @GetMapping("/calculateOrderRealDistance/{orderId}")
    public Result<BigDecimal> calculateOrderRealDistance(@PathVariable Long orderId) {
        return Result.ok(locationService.calculateOrderRealDistance(orderId));
    }
}

