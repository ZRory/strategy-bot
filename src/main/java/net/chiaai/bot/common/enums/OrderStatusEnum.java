package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    NEW("新建订单"),
    PARTIALLY_FILLED("部分成交"),
    FILLED("全部成交"),
    CANCELED("已撤销"),
    REJECTED("订单被拒绝"),
    EXPIRED("订单过期(根据timeInForce参数规则)");

    private String desc;

}
