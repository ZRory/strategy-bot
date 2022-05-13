package net.chiaai.bot.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Indicator {

    /**
     * 开盘时间戳 （唯一键）
     */
    private Long openTime;

    /**
     * shortEMA-longEMA
     */
    private BigDecimal dif;

    /**
     * (pre(dea)*8/10) + (dif*2/10)
     */
    private BigDecimal dea;

    /**
     * dif-dea
     */
    private BigDecimal macd;

}
