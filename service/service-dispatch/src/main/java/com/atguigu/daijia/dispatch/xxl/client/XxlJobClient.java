package com.atguigu.daijia.dispatch.xxl.client;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.xxl.config.XxlJobClientConfig;
import com.atguigu.daijia.model.entity.dispatch.XxlJobInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

 /*
  * @Title: 调用调度中心中的方法
  * @Author: pyzxW
  * @Date: 2025-03-29 20:13:45
  * @Params:
  * @Return: null
  * @Description: 执行器中的具体方法编写
  */
@Slf4j
@Component
public class XxlJobClient {

    @Autowired
    private XxlJobClientConfig xxlJobClientConfig;

    //客户端调用服务端中的方法
    @Autowired
    private RestTemplate restTemplate;

     /*
      * @Title: addJob
      * @Author: pyzxW
      * @Date: 2025-03-29 20:14:04
      * @Params:
      * @Return: null
      * @Description: 添加任务
      */
    @SneakyThrows
    public Long addJob(String executorHandler, String param, String corn, String desc){
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("qy");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getAddUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if(response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("增加xxl执行任务成功,返回信息:{}", response.getBody().toJSONString());
            //content为任务id
            return response.getBody().getLong("content");
        }
        log.info("调用xxl增加执行任务失败:{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    public Boolean startJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getStartJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if(response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("启动xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        log.info("启动xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    public Boolean stopJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getStopJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if(response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("停止xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        log.info("停止xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    public Boolean removeJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getRemoveUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if(response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("删除xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        log.info("删除xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

     /*
      * @Title: addAndStart
      * @Author: pyzxW
      * @Date: 2025-03-29 20:17:28
      * @Params:
      * @Return: null
      * @Description: 添加并且启动任务,并且任务会启动一次
      */
    public Long addAndStart(String executorHandler, String param, String corn, String desc) {
        //创建对象
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("qy");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        //创建头信息
        //创建请求
        HttpHeaders headers = new HttpHeaders();
        //设置类型
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        //获取调度中心的请求路径
        String url = xxlJobClientConfig.getAddAndStartUrl();

        //restTemplate进行请求,注意要在启动类中配置对应的启动项
        //返回对应的请求
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);

        //状态码
        if(response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("增加并开始执行xxl任务成功,返回信息:{}", response.getBody().toJSONString());
            //content为任务id
            return response.getBody().getLong("content");
        }
        log.info("增加并开始执行xxl任务失败:{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }
//    public Long addAndStart(String executorHandler, String param, String corn, String desc) {
//        XxlJobInfo xxlJobInfo = new XxlJobInfo();
//        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
//        xxlJobInfo.setJobDesc(desc);
//        xxlJobInfo.setAuthor("qy");
//        xxlJobInfo.setScheduleType("CRON");
//        xxlJobInfo.setScheduleConf(corn);
//        xxlJobInfo.setGlueType("BEAN");
//        xxlJobInfo.setExecutorHandler(executorHandler);
//        xxlJobInfo.setExecutorParam(param);
//        xxlJobInfo.setExecutorRouteStrategy("FIRST");
//        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
//        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
//        xxlJobInfo.setExecutorTimeout(0);
//        xxlJobInfo.setExecutorFailRetryCount(0);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);
//
//        String url = xxlJobClientConfig.getAddAndStartUrl();
//        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
//        if(response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
//            log.info("增加并开始执行xxl任务成功,返回信息:{}", response.getBody().toJSONString());
//            //content为任务id
//            return response.getBody().getLong("content");
//        }
//        log.info("增加并开始执行xxl任务失败:{}", response.getBody().toJSONString());
//        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//    }
}
