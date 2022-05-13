package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceAmountRateResponse extends BaseResponse {

    private String symbol;

    /**
     * 挂单手续费
     */
    private BigDecimal makerCommissionRate;

    /**
     * 吃单手续费
     */
    private BigDecimal takerCommissionRate;

}
