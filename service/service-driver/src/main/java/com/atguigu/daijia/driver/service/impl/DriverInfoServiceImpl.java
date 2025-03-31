package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.mapper.*;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.*;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private DriverInfoMapper driverInfoMapper;

    @Autowired
    private DriverSetMapper driverSetMapper;

    @Autowired
    private DriverAccountMapper driverAccountMapper;

    @Autowired
    private DriverLoginLogMapper driverLoginLogMapper;

    @Autowired
    private DriverFaceRecognitionMapper driverFaceRecognitionMapper;

    @Autowired
    private CosService cosService;

    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    //小程序授权登录
    @Override
    public Long login(String code) {
        try {
            //根据code + 小程序id + 秘钥请求微信接口，返回openid
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            //获取openid
            String openid = sessionInfo.getOpenid();

            //根据openid查询是否第一次登录
            LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
            //在数据库中进行对比,确实有在这个信息则拿出来
            wrapper.eq(DriverInfo::getWxOpenId,openid);
            DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);
            //如果是第一次登录，添加司机基本信息
            if(driverInfo == null) {
                //1.添加司机基本信息
                driverInfo = new DriverInfo();
                driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
                driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                driverInfo.setWxOpenId(openid);
                driverInfoMapper.insert(driverInfo);

                //2.初始化司机设置
                DriverSet driverSet = new DriverSet();
                driverSet.setDriverId(driverInfo.getId());
                driverSet.setOrderDistance(new BigDecimal(0));//0：无限制
                driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE));//默认接单范围：5公里
                driverSet.setIsAutoAccept(0);//0：否 1：是 //自动接单操作
                //调用mapper中的插入操作进行添加
                driverSetMapper.insert(driverSet);

                //3.初始化司机账户信系
                DriverAccount driverAccount = new DriverAccount();
                driverAccount.setDriverId(driverInfo.getId());
                driverAccountMapper.insert(driverAccount);
            }
            //记录司机的登录信息
            DriverLoginLog driverLoginLog = new DriverLoginLog();
            driverLoginLog.setDriverId(driverInfo.getId());
            driverLoginLog.setMsg("小程序登录");
            driverLoginLogMapper.insert(driverLoginLog);

            //返回司机的id
            return driverInfo.getId();
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

//    @Override
//    public Long login(String code) {
//        try {
//            //根据code + 小程序id + 秘钥请求微信接口，返回openid
//            WxMaJscode2SessionResult sessionInfo =
//                    wxMaService.getUserService().getSessionInfo(code);
//            String openid = sessionInfo.getOpenid();
//
//            //根据openid查询是否第一次登录
//            LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(DriverInfo::getWxOpenId,openid);
//            DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);
//
//            if(driverInfo == null) {
//                //添加司机基本信息
//                driverInfo = new DriverInfo();
//                driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
//                driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
//                driverInfo.setWxOpenId(openid);
//                driverInfoMapper.insert(driverInfo);
//
//                //初始化司机设置
//                DriverSet driverSet = new DriverSet();
//                driverSet.setDriverId(driverInfo.getId());
//                driverSet.setOrderDistance(new BigDecimal(0));//0：无限制
//                driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE));//默认接单范围：5公里
//                driverSet.setIsAutoAccept(0);//0：否 1：是
//                driverSetMapper.insert(driverSet);
//
//                //初始化司机账户信息
//                DriverAccount driverAccount = new DriverAccount();
//                driverAccount.setDriverId(driverInfo.getId());
//                driverAccountMapper.insert(driverAccount);
//            }
//
//            //记录司机登录信息
//            DriverLoginLog driverLoginLog = new DriverLoginLog();
//            driverLoginLog.setDriverId(driverInfo.getId());
//            driverLoginLog.setMsg("小程序登录");
//            driverLoginLogMapper.insert(driverLoginLog);
//
//            //返回司机id
//            return driverInfo.getId();
//        } catch (WxErrorException e) {
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//    }



    //获取司机登录信息
//    @Override
//    public DriverLoginVo getDriverInfo(Long driverId) {
//        //根据司机的ID获取司机的信息
//        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
//        //为了返回DriverLoginVo对象，使用批量赋值操作
//        DriverLoginVo driverLoginVo = new DriverLoginVo();
//        BeanUtils.copyProperties(driverInfo,driverLoginVo);
//
//        //判断是否需要建档人脸识别
//        String faceModelId = driverInfo.getFaceModelId();
//        boolean isArchiveFace = StringUtils.hasText(faceModelId);
//        driverLoginVo.setIsArchiveFace(isArchiveFace);
//        //返回信息
//        return driverLoginVo;
//    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        //根据司机id获取司机信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        //driverInfo -- DriverLoginVo
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo,driverLoginVo);

        //是否建档人脸识别
        String faceModelId = driverInfo.getFaceModelId();
        boolean isArchiveFace = StringUtils.hasText(faceModelId);
        driverLoginVo.setIsArchiveFace(isArchiveFace);
        return driverLoginVo;
    }

    //获取司机认证信息
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        //赋值新vo
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        BeanUtils.copyProperties(driverInfo,driverAuthInfoVo);

        //图片地址生成,临时地址
        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
        return driverAuthInfoVo;
    }
//    @Override
//    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
//        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
//        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
//        BeanUtils.copyProperties(driverInfo,driverAuthInfoVo);
//
//        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
//        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
//        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
//        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
//        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
//        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
//
//        return driverAuthInfoVo;
//    }


    //更新司机认证信息
     /*
      * @Title: updateDriverAuthInfo
      * @Author: pyzxW
      * @Date: 2025-03-16 15:58:11
      * @Params: [updateDriverAuthInfoForm]
      * @Return: Boolean
      * @Description: 更新司机认证信息
      */
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        //获取司机id
        Long driverId = updateDriverAuthInfoForm.getDriverId();

        //修改操作
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(driverId);
        //这里的copy操作必须有id进行识别操作，才方便进行复制
        BeanUtils.copyProperties(updateDriverAuthInfoForm,driverInfo);

//        int i = driverInfoMapper.updateById(driverInfo);
        //布尔值
        boolean update = this.updateById(driverInfo);
        return update;
    }

    //创建司机人脸模型
     /*
      * @Title: creatDriverFaceModel
      * @Author: pyzxW
      * @Date: 2025-03-17 15:18:15
      * @Params: [driverFaceModelForm]
      * @Return: Boolean
      * @Description: 创建司机人脸模型
      */
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        //根据司机id获取司机信息
        //直接可以从传入的参数中获取对应的值
        DriverInfo driverInfo =
                driverInfoMapper.selectById(driverFaceModelForm.getDriverId());
        try{

            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取

            //获取对应的ID和秘钥
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                                            tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            //传入地域
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                                                     clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreatePersonRequest req = new CreatePersonRequest();
            //设置相关值
            req.setGroupId(tencentCloudProperties.getPersionGroupId());
            //基本信息
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());

            // 返回的resp是一个CreatePersonResponse的实例，与请求对象对应
            CreatePersonResponse resp = client.CreatePerson(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            String faceId = resp.getFaceId();
            //值不为空则进行更新
            if(StringUtils.hasText(faceId)) {
                driverInfo.setFaceModelId(faceId);
                driverInfoMapper.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

     /*
      * @Title: getDriverSet
      * @Author: pyzxW
      * @Date: 2025-03-23 19:53:58
      * @Params:
      * @Return: null
      * @Description: 获取司机设置信息
      */
    //获取司机设置信息
    @Override
    public DriverSet getDriverSet(Long driverId) {
        //mybatis的查询条件
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        //比对id信息
        wrapper.eq(DriverSet::getDriverId,driverId);
        //查询数据库中是否有该司机的设置信息
        DriverSet driverSet = driverSetMapper.selectOne(wrapper);
        return driverSet;
    }

    ////判断司机当日是否进行过人脸识别
     /*
      * @Title: isFaceRecognition
      * @Author: pyzxW
      * @Date: 2025-03-31 14:40:12
      * @Params: [driverId]
      * @Return: Boolean
      * @Description: 判断司机当日是否进行过人脸识别
      */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        //根据司机id以及当日的日期进行查询
        LambdaQueryWrapper<DriverFaceRecognition> wrapper = new LambdaQueryWrapper<>();
        //司机id判断
        wrapper.eq(DriverFaceRecognition::getDriverId,driverId);
        //获取当日的日期。注意年月日之排序
        wrapper.eq(DriverFaceRecognition::getFaceDate,new DateTime().toString("yyyy-MM-dd"));
        //调用mapper进行实现
        //人脸识别次数
        Long count = driverFaceRecognitionMapper.selectCount(wrapper);
//        driverFaceRecognitionMapper.selectOne(wrapper);用于条件查询单条记录。
//        driverFaceRecognitionMapper.selectById(wrapper);selectById 方法的参数为主键值,主键值查询

        return count != 0;
    }

    ////判断司机当日是否进行过人脸识别
//    @Override
//    public Boolean isFaceRecognition(Long driverId) {
//        //根据司机id + 当日日期进行查询
//        LambdaQueryWrapper<DriverFaceRecognition> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DriverFaceRecognition::getDriverId,driverId);
//        // 年-月-日 格式
//        wrapper.eq(DriverFaceRecognition::getFaceDate,new DateTime().toString("yyyy-MM-dd"));
//        //调用mapper方法
//        Long count = driverFaceRecognitionMapper.selectCount(wrapper);
//
//        return count != 0;
//    }

     /*
      * @Title: verifyDriverFace
      * @Author: pyzxW
      * @Date: 2025-03-31 15:08:36
      * @Params: [driverFaceModelForm]
      * @Return: Boolean
      * @Description: 简单人脸识别
      */
    //人脸识别
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        //1 照片比对
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            VerifyFaceRequest req = new VerifyFaceRequest();
            //设置相关参数
            //设置照片尺寸、个人之ID
            req.setImage(driverFaceModelForm.getImageBase64());
            req.setPersonId(String.valueOf(driverFaceModelForm.getDriverId()));

            // 返回的resp是一个VerifyFaceResponse的实例，与请求对象对应
            VerifyFaceResponse resp = client.VerifyFace(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            if(resp.getIsMatch()) { //照片比对成功
                //2 如果照片比对成功，静态活体检测
                Boolean isSuccess = this.detectLiveFace(driverFaceModelForm.getImageBase64());
                if(isSuccess) {//3 如果静态活体检测通过，添加数据到认证表里面
                    DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
                    //设置相关参数如数据库
                    driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
                    driverFaceRecognition.setFaceDate(new Date());
                    driverFaceRecognitionMapper.insert(driverFaceRecognition);
                    return true;
                }
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }

        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    /*
     * @Title: detectLiveFace
     * @Author: pyzxW
     * @Date: 2025-03-31 15:15:07
     * @Params:
     * @Return: null
     * @Description: 人脸静态活体检测
     */
    //人脸静态活体检测
    private Boolean detectLiveFace(String imageBase64) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(imageBase64);
            // 返回的resp是一个DetectLiveFaceResponse的实例，与请求对象对应
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);
            // 输出json格式的字符串回包
            System.out.println(DetectLiveFaceResponse.toJsonString(resp));
            if(resp.getIsLiveness()) {
                return true;
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        return false;
    }

    //更新接单状态
    // update driver_set set status=? where driver_id=?
    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId,driverId);
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);
        driverSetMapper.update(driverSet,wrapper);
        return true;
    }

    //获取司机基本信息
    @Override
    public DriverInfoVo getDriverInfoOrder(Long driverId) {
        //司机id获取基本信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        //封装DriverInfoVo
        DriverInfoVo driverInfoVo = new DriverInfoVo();
        BeanUtils.copyProperties(driverInfo,driverInfoVo);

        //计算驾龄
        //获取当前年
        int currentYear = new DateTime().getYear();
        //获取驾驶证初次领证日期
        //driver_license_issue_date
        int firstYear = new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear();
        int driverLicenseAge = currentYear - firstYear;
        driverInfoVo.setDriverLicenseAge(driverLicenseAge);

        return driverInfoVo;
    }

    @Override
    public String getDriverOpenId(Long driverId) {
        DriverInfo driverInfo = this.getOne(new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getId, driverId).select(DriverInfo::getWxOpenId));
        return driverInfo.getWxOpenId();
    }

}