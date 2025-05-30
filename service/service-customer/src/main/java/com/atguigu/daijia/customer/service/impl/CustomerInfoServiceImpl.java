package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    @Autowired
    private WxMaService wxMaService;
    
    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Autowired
    private CustomerLoginLogMapper customerLoginLogMapper;

    //微信小程序登录接口
    @Override
    public Long login(String code) {
        //1 获取code值，使用微信工具包对象，获取微信唯一标识openid
        String openid = null;
        try {
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(code);
            openid = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }

        //2 根据openid查询数据库表，判断是否第一次登录
        //如果openid不存在返回null，如果存在返回一条记录
        //select * from customer_info ci where ci.wx_open_id = ''
        //数据库查询操作
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        //查询表中的WxOpenId是否等于传入的openid
        wrapper.eq(CustomerInfo::getWxOpenId,openid);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);

        //3 如果第一次登录，添加信息到用户表
        if(customerInfo == null) {
            customerInfo = new CustomerInfo();
            //添加默认昵称
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            //添加头像
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setWxOpenId(openid);
            customerInfoMapper.insert(customerInfo);
        }

        //4 记录登录日志信息
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);

        //5 返回用户id
        return customerInfo.getId();
    }

     /*
      * @Title: getCustomerInfo
      * @Author: pyzxW
      * @Date: 2025-05-12 15:47:44
      * @Params:
      * @Return: null
      * @Description: 获取用户的信息
      */
    //获取客户登录信息
    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        //1 根据用户id查询用户信息
        //调用mp中查询的方法
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);

        //2 封装到CustomerLoginVo
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        //customerLoginVo.setNickname(customerInfo.getNickname());
        BeanUtils.copyProperties(customerInfo,customerLoginVo);

        //判断是否需要绑定手机号
        //@Schema(description = "是否绑定手机号码")
        //    private Boolean isBindPhone;
        String phone = customerInfo.getPhone();
        boolean isBindPhone = StringUtils.hasText(phone);
        customerLoginVo.setIsBindPhone(isBindPhone);

        //3 CustomerLoginVo返回
        return customerLoginVo;
    }

    ////更新客户微信手机号码
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        //1 根据code值获取微信绑定手机号码
        try {
            //调用微信方法
            WxMaPhoneNumberInfo phoneNoInfo =
                    wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
            //获取对应的手机号
            String phoneNumber = phoneNoInfo.getPhoneNumber();

            //更新用户信息
            //获取用户之id
            Long customerId = updateWxPhoneForm.getCustomerId();
            //查询id对应的数据库信息
            CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
            //更新对应的数据库中的用户手机号码
            customerInfo.setPhone(phoneNumber);
            //更新操作
            customerInfoMapper.updateById(customerInfo);
            //返回正确更新
            return true;
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

     /*
      * @Title: getCustomerOpenId
      * @Author: pyzxW
      * @Date: 2025-05-12 15:48:00
      * @Params:
      * @Return: null
      * @Description: 获取客户的openid信息
      */
    @Override
    public String getCustomerOpenId(Long customerId) {
        //查询CustomerInfo库
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getId,customerId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
        return customerInfo.getWxOpenId();

//        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(CustomerInfo::getId,customerId);
//        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
//        return customerInfo.getWxOpenId();
    }

}
