package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudProperties {
    //秘钥ID
    private String secretId;
    //秘钥
    private String secretKey;
    //地域
    private String region;
    //存储桶名字
    private String bucketPrivate;
    //人脸识别操作ID
    private String persionGroupId;
}
