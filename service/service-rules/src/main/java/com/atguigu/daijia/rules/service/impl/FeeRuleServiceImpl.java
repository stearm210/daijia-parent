package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.mapper.FeeRuleMapper;
import com.atguigu.daijia.rules.service.FeeRuleService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {

    @Autowired
    private KieContainer kieContainer;

     /*
      * @Title: calculateOrderFee
      * @Author: pyzxW
      * @Date: 2025-03-21 20:44:27
      * @Params:
      * @Return: null
      * @Description: 计算里程费用
      */
    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
        //1. 封装输入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        //里程
        feeRuleRequest.setDistance(calculateOrderFeeForm.getDistance());
        //代驾时间
        Date startTime = calculateOrderFeeForm.getStartTime();
        feeRuleRequest.setStartTime(new DateTime(startTime).toString("HH:mm:ss"));
        //等待时间
        feeRuleRequest.setWaitMinute(calculateOrderFeeForm.getWaitMinute());


        //2.Drools使用
        KieSession kieSession = kieContainer.newKieSession();
        //封装返回对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        //设置全局变量
        kieSession.setGlobal("feeRuleResponse",feeRuleResponse);
        //插入对象
        kieSession.insert(feeRuleRequest);
        //触发对应规则
        kieSession.fireAllRules();
        //规则终止
        kieSession.dispose();


        //3.封装数据到FeeRuleResponseVo中返回
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        BeanUtils.copyProperties(feeRuleResponse,feeRuleResponseVo);
        return feeRuleResponseVo;
    }

    //计算订单费用
//    @Override
//    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
//
//        //封装输入对象
//        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
//        feeRuleRequest.setDistance(calculateOrderFeeForm.getDistance());
//        Date startTime = calculateOrderFeeForm.getStartTime();
//        feeRuleRequest.setStartTime(new DateTime(startTime).toString("HH:mm:ss"));
//        feeRuleRequest.setWaitMinute(calculateOrderFeeForm.getWaitMinute());
//
//        //Drools使用
//        KieSession kieSession = kieContainer.newKieSession();
//
//        //封装返回对象
//        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
//        kieSession.setGlobal("feeRuleResponse",feeRuleResponse);
//
//        kieSession.insert(feeRuleRequest);
//        kieSession.fireAllRules();
//        kieSession.dispose();
//
//        //封装数据到FeeRuleResponseVo返回
//        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
//        // feeRuleResponse -- feeRuleResponseVo
//        BeanUtils.copyProperties(feeRuleResponse,feeRuleResponseVo);
//        return feeRuleResponseVo;
//    }
}
