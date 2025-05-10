package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

 /*
  * @Title:
  * @Author: pyzxW
  * @Date: 2025-04-19 15:50:39
  * @Params:
  * @Return: null
  * @Description: mino之配置信息
  */
 @Configuration
 @ConfigurationProperties(prefix="minio") //读取节点
 @Data
 public class MinioProperties {

     private String endpointUrl;
     private String accessKey;
     private String secreKey;
     private String bucketName;
 }