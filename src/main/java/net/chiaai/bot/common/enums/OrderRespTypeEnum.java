package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 下单接口支持新的可选参数 newOrderRespType 表示下单响应类型。支持ACK 和 RESULT,
 * 如果newOrderRespType= RESULT:
 * MARKET 订单将直接返回成交(FILLED)结果；
 * 配合使用特殊 timeInForce 的 LIMIT 订单将直接返回成交/过期(FILLED/EXPIRED)结果。
 */
@Getter
@AllArgsConstructor
public enum OrderRespTypeEnum {

    ACK("ACK"),
    RESULT("RESULT");

    private String desc;

}
