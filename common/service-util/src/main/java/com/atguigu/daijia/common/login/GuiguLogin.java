package com.atguigu.daijia.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//自定义注解
//登录判断
//方法上可以使用这个注解
@Target(ElementType.METHOD)
//作用的范围是运行阶段起作用
@Retention(RetentionPolicy.RUNTIME)
public @interface GuiguLogin {

}
