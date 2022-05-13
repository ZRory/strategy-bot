package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceResponse extends BaseResponse {

    private String symbol;

    private BigDecimal price;

    private Long time;

}
