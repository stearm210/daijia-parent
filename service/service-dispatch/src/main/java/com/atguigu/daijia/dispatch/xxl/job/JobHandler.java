package com.atguigu.daijia.dispatch.xxl.job;

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobHandler {

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private NewOrderService newOrderService;

     /*
      * @Title: newOrderTaskHandler
      * @Author: pyzxW
      * @Date: 2025-03-30 15:02:59
      * @Params:
      * @Return: null
      * @Description: 日志信息记录
      */
    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {

        //记录任务调度日志
        XxlJobLog xxlJobLog = new XxlJobLog();
        //获取当前执行的jobid信息，存入临时变量中
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();

        try {
            //执行任务：搜索附近代驾司机
            newOrderService.executeTask(XxlJobHelper.getJobId());

            //成功状态
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            //失败状态
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        } finally {
            long times = System.currentTimeMillis()- startTime;
            //TODO 完善long
            xxlJobLog.setTimes((int)times);
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
