package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;
import net.chiaai.bot.common.enums.FilterTypeEnum;

import java.math.BigDecimal;

@Getter
@Setter
public class Filter {

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private FilterTypeEnum filterType;
    private BigDecimal tickSize;
    private BigDecimal stepSize;
    private BigDecimal maxQty;
    private BigDecimal minQty;
    private BigDecimal limit;
    private BigDecimal notional;
    private BigDecimal multiplierDown;
    private BigDecimal multiplierUp;
    private BigDecimal multiplierDecimal;

}
