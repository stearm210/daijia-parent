package com.atguigu.daijia.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

 /*
  * @Title:
  * @Author: pyzxW
  * @Date: 2025-03-21 20:10:09
  * @Params:
  * @Return: null
  * @Description: 主要用于封装对应的数据
  */
@Data
public class FeeRuleRequest {

    @Schema(description = "代驾里程")
    private BigDecimal distance;

    @Schema(description = "代驾时间")
    private String startTime;

    @Schema(description = "等候分钟")
    private Integer waitMinute;

}
