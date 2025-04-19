package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "监控接口管理")
@RestController
@RequestMapping(value="/monitor")
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

     /*
      * @Title: upload
      * @Author: pyzxW
      * @Date: 2025-04-19 16:54:36
      * @Params:  
      * @Return: null
      * @Description: 上传录音文件
      */
    @Operation(summary = "上传录音")
    @PostMapping("/upload")
    public Result<Boolean> upload(@RequestParam("file") MultipartFile file,
                                  OrderMonitorForm orderMonitorForm) {

        return Result.ok(monitorService.upload(file, orderMonitorForm));
    }
}

