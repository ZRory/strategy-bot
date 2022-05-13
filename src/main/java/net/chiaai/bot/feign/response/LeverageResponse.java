package net.chiaai.bot.feign.response;

import lombok.Data;

@Data
public class LeverageResponse extends BaseResponse {

    /**
     * 杠杆倍数
     */
    private Integer leverage;

    /**
     * 当前杠杆倍数下允许的最大名义价值
     */
    private String maxNotionalValue;

    /**
     * 交易对
     */
    private String symbol;

}
