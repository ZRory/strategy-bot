package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExchangeInfoResp extends BaseResponse {

    private String timezone;
    private long serverTime;
    private String futuresType;
    private List<RateLimit> rateLimits;
    private List<String> exchangeFilters;
    private List<Asset> assets;
    private List<Symbol> symbols;

}
