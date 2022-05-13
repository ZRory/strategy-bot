package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;
import net.chiaai.bot.common.enums.SymbolStatusEnum;

import java.util.List;

@Getter
@Setter
public class Symbol {

    private String symbol;
    private String pair;
    private String contractType;
    private Long deliveryDate;
    private Long onboardDate;
    private SymbolStatusEnum status;
    private String maIntegerMarginPercent;
    private String requiredMarginPercent;
    private String baseAsset;
    private String quoteAsset;
    private String marginAsset;
    private Integer pricePrecision;
    private Integer quantityPrecision;
    private Integer baseAssetPrecision;
    private Integer quotePrecision;
    private String underlyingType;
    private List<String> underlyingSubType;
    private Long settlePlan;
    private String triggerProtect;
    private String liquidationFee;
    private String marketTakeBound;
    private List<Filter> filters;
    private List<String> orderTypes;
    private List<String> timeInForce;

}
