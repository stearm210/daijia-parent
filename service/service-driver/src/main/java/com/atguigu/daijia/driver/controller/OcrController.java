package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.OcrService;
import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云识别接口管理")
@RestController
@RequestMapping(value="/ocr")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrController {
	
    @Autowired
    private OcrService ocrService;

     /*
      * @Title: idCardOcr
      * @Author: pyzxW
      * @Date: 2025-03-11 16:00:33
      * @Params:
      * @Return: null
      * @Description: 身份证识别操作
      */
    @Operation(summary = "身份证识别")
    @PostMapping("/idCardOcr")
    public Result<IdCardOcrVo> idCardOcr(@RequestPart("file") MultipartFile file) {
        IdCardOcrVo idCardOcrVo = ocrService.idCardOcr(file);
        return Result.ok(idCardOcrVo);
    }

     /*
      * @Title: driverLicenseOcr
      * @Author: pyzxW
      * @Date: 2025-03-12 15:04:45
      * @Params:
      * @Return: null
      * @Description: 驾驶证识别
      */
    @Operation(summary = "驾驶证识别")
    @PostMapping("/driverLicenseOcr")
    public Result<DriverLicenseOcrVo> driverLicenseOcr(@RequestPart("file") MultipartFile file) {
        return Result.ok(ocrService.driverLicenseOcr(file));
    }
}

