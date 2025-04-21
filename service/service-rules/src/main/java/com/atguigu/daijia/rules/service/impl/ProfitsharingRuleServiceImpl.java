package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequest;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponse;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.rules.mapper.ProfitsharingRuleMapper;
import com.atguigu.daijia.rules.service.ProfitsharingRuleService;
import com.atguigu.daijia.rules.utils.DroolsHelper;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProfitsharingRuleServiceImpl implements ProfitsharingRuleService {

    @Autowired
    private ProfitsharingRuleMapper rewardRuleMapper;

    private static final String RULES_CUSTOMER_RULES_DRL = "rules/ProfitsharingRule.drl";

     /*
      * @Title: calculateOrderProfitsharingFee
      * @Author: pyzxW
      * @Date: 2025-04-21 17:32:12
      * @Params:
      * @Return: null
      * @Description: 计算系统分账费用
      */
    @Override
    public ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        //传入参数对象封装
        ProfitsharingRuleRequest profitsharingRuleRequest = new ProfitsharingRuleRequest();
        //订单的金额
        profitsharingRuleRequest.setOrderAmount(profitsharingRuleRequestForm.getOrderAmount());
        //订单的数量
        profitsharingRuleRequest.setOrderNum(profitsharingRuleRequestForm.getOrderNum());

        //创建kieSession
        KieSession kieSession = DroolsHelper.loadForRule(RULES_CUSTOMER_RULES_DRL);

        //封装返回对象
        ProfitsharingRuleResponse profitsharingRuleResponse = new ProfitsharingRuleResponse();
        //规则文件中定义的全局变量
        kieSession.setGlobal("profitsharingRuleResponse",profitsharingRuleResponse);

        //触发规则，返回vo对象
        kieSession.insert(profitsharingRuleRequest);
        kieSession.fireAllRules();//触发规则

        kieSession.dispose();

        //设置对应值
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = new ProfitsharingRuleResponseVo();
        BeanUtils.copyProperties(profitsharingRuleResponse,profitsharingRuleResponseVo);

        return profitsharingRuleResponseVo;
    }
}
