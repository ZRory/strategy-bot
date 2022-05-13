package net.chiaai.bot.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BollBand {

    /**
     * 开盘时间戳 （唯一键）
     */
    private Long openTime;

    /**
     * BOLL上轨
     */
    private BigDecimal upper;

    /**
     * BOLL中轨
     */
    private BigDecimal middle;

    /**
     * BOLL下轨
     */
    private BigDecimal lower;

    /**
     * 标准差MD
     */
    private BigDecimal md;

}
