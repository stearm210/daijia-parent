package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

     /*
      * @Title: updateDriverLocation
      * @Author: pyzxW
      * @Date: 2025-03-23 19:41:58
      * @Params:  
      * @Return: null
      * @Description: 更新司机的位置信息
      */
    //更新司机位置
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        //开启接单了才能更新司机接单位置
        DriverSet driverSet = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId()).getData();
        if(driverSet.getServiceStatus().intValue() == 1) {
            return locationFeignClient.updateDriverLocation(updateDriverLocationForm).getData();
        } else {
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }
        //根据司机id获取司机个性化设置信息
//        Long driverId = updateDriverLocationForm.getDriverId();
//        Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(driverId);
//        DriverSet driverSet = driverSetResult.getData();
//
//        //判断：如果司机开始接单，更新位置信息
//        if(driverSet.getServiceStatus() == 1) {
//            Result<Boolean> booleanResult = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
//            return booleanResult.getData();
//        } else {
//            //没有接单
//            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
//        }
    }

     /*
      * @Title: updateOrderLocationToCache
      * @Author: pyzxW
      * @Date: 2025-04-13 15:58:30
      * @Params:
      * @Return: null
      * @Description: 司机前往代驾地点之更新位置到redis
      */
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
    }

     /*
      * @Title: saveOrderServiceLocation
      * @Author: pyzxW
      * @Date: 2025-04-18 17:23:57
      * @Params:
      * @Return: null
      * @Description: 开始代驾服务
      */
    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return locationFeignClient.saveOrderServiceLocation(orderLocationServiceFormList).getData();
    }
}
